package com.gamelog.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Leva os eventos da tabela de outbox pro RabbitMQ.
//
// === Garantia de entrega: "pelo menos uma vez" ===
//
// Um evento so e marcado como publicado depois que o BROKER confirma que recebeu
// (publisher confirm). Se o monolito cair entre o envio e a marcacao, o evento sai
// de novo no proximo ciclo - duplicado. E o preco de nunca perder: "exatamente uma
// vez" entre dois sistemas sem transacao distribuida nao existe. Os consumidores
// lidam com a duplicata guardando o eventId do que ja processaram.
//
// === Ordem ===
//
// Os eventos saem na ordem do id da tabela, e o ciclo PARA no primeiro que falhar.
// Pular o que falhou e seguir com os outros entregaria "review apagada" antes de
// "review criada", e o consumidor ficaria com uma avaliacao fantasma.
@Component
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final long confirmTimeoutMs;

    public OutboxRelay(OutboxRepository outboxRepository,
                       RabbitTemplate rabbitTemplate,
                       Clock clock,
                       ObjectProvider<MeterRegistry> meterRegistry,
                       @Value("${app.outbox.relay.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.confirmTimeoutMs = confirmTimeoutMs;

        // Quantos eventos estao esperando pra sair. Parado em zero e o normal;
        // subindo sem parar significa broker fora do ar ou relay travado - e a
        // metrica que um alerta de producao observaria.
        meterRegistry.ifAvailable(registry -> Gauge
                .builder("gamelog.outbox.pending", outboxRepository, OutboxRepository::countByPublishedAtIsNull)
                .description("Eventos gravados no outbox e ainda nao confirmados pelo broker")
                .register(registry));
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.interval-ms:500}")
    public void relayPending() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();

        for (OutboxEvent event : pending) {
            if (!deliver(event)) {
                // Para no primeiro que falhou, pra preservar a ordem. O proximo ciclo
                // recomeca exatamente deste evento.
                return;
            }
        }
    }

    // Tira do banco o que ja foi entregue ha mais de uma semana.
    @Scheduled(cron = "${app.outbox.cleanup-cron:0 0 4 * * *}")
    public void cleanUp() {
        int removed = outboxRepository.deletePublishedBefore(clock.instant().minus(Duration.ofDays(7)));
        if (removed > 0) {
            log.info("Outbox: {} eventos antigos removidos", removed);
        }
    }

    boolean deliver(OutboxEvent event) {
        CorrelationData correlation = new CorrelationData(event.getEventId());

        try {
            rabbitTemplate.send(Messaging.EVENTS_EXCHANGE, event.getEventType(), toMessage(event), correlation);

            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("broker recusou a mensagem: " + confirm.getReason());
            }

            event.markPublished(clock.instant());
            outboxRepository.save(event);
            log.debug("Evento {} ({}) publicado", event.getEventId(), event.getEventType());
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            event.registerFailure(e.toString());
            outboxRepository.save(event);
            log.warn("Falha ao publicar evento {} ({}), tentativa {}: {}. Fica no outbox para o proximo ciclo.",
                    event.getEventId(), event.getEventType(), event.getAttempts(), e.toString());
            return false;
        }
    }

    // O corpo vai exatamente como foi gravado no commit. Os metadados que o
    // consumidor usa sem abrir o JSON (id, tipo, momento) vao nas propriedades
    // padrao do AMQP, que o painel do RabbitMQ e qualquer ferramenta sabem ler.
    private Message toMessage(OutboxEvent event) {
        return MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getEventId())
                .setType(event.getEventType())
                .setTimestamp(Date.from(event.getOccurredAt()))
                .setAppId(OutboxEventPublisher.SOURCE)
                // Persistente: sobrevive a um restart do broker enquanto estiver
                // numa fila duravel.
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
