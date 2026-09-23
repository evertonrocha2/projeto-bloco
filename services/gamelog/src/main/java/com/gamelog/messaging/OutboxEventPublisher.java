package com.gamelog.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Implementacao padrao do EventPublisher: grava o evento na tabela de outbox.
//
// Nao fala com o RabbitMQ. Isso e o ponto: o service de negocio termina a
// transacao dele sem depender do broker estar no ar. Com o RabbitMQ fora, o
// usuario continua conseguindo publicar review - os eventos esperam na tabela e
// saem quando o broker voltar (ver OutboxRelay).
//
// Propagation.REQUIRED: dentro de uma transacao de negocio, entra NELA (e o caso
// normal - ReviewService.create). Chamado de fora de transacao, como no seeder,
// abre uma propria. MANDATORY seria mais rigoroso, mas obrigaria o seeder a virar
// transacional so pra publicar o catalogo inicial.
@Component
public class OutboxEventPublisher implements EventPublisher {

    public static final String SOURCE = "gamelog";
    public static final int SCHEMA_VERSION = 1;

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    // Opcional: sem tracing configurado (testes, ou management.tracing.enabled=false)
    // o evento sai sem trace e tudo continua funcionando.
    private final ObjectProvider<Tracer> tracer;

    public OutboxEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper, Clock clock,
                                ObjectProvider<Tracer> tracer) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tracer = tracer;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(String eventType, String aggregateId, Object payload) {
        IntegrationEvent event = new IntegrationEvent(
                UUID.randomUUID().toString(),
                eventType,
                SCHEMA_VERSION,
                clock.instant(),
                SOURCE,
                aggregateId,
                payload);

        OutboxEvent row = new OutboxEvent(
                event.eventId(), eventType, aggregateId, serialize(event), event.occurredAt());

        Tracer current = tracer.getIfAvailable();
        Span span = current == null ? null : current.currentSpan();
        if (span != null) {
            row.attachTrace(span.context().traceId(), span.context().spanId());
        }

        outboxRepository.save(row);
    }

    private String serialize(IntegrationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // Falha de serializacao e bug de programacao (payload que o Jackson nao
            // sabe escrever), nao falha de infraestrutura. Estourar aqui desfaz a
            // transacao inteira, e e o certo: melhor recusar a review do que
            // grava-la sem o evento que outros servicos esperam.
            throw new IllegalStateException("Nao foi possivel serializar o evento " + event.eventType(), e);
        }
    }
}
