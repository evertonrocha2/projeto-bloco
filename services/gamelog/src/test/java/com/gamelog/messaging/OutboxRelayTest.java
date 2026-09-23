package com.gamelog.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;

// O lado "entregar" do outbox, com o broker simulado.
//
// Tres comportamentos que definem a garantia de entrega:
//  - so marca como publicado com confirmacao positiva do broker;
//  - falha deixa o evento pendente, com o erro anotado;
//  - para no primeiro que falhou, pra nao entregar fora de ordem.
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    private OutboxRepository repository;
    private RabbitTemplate rabbitTemplate;
    private OutboxRelay relay;
    private final List<String> sentRoutingKeys = new ArrayList<>();
    private final List<Message> sentMessages = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OutboxRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        relay = new OutboxRelay(repository, rabbitTemplate, Clock.fixed(NOW, ZoneOffset.UTC),
                mock(ObjectProvider.class), 1000);
    }

    private static OutboxEvent event(String id, String type) {
        return new OutboxEvent(id, type, "1", "{\"eventId\":\"" + id + "\"}", NOW.minusSeconds(5));
    }

    // Faz o broker responder ack/nack a cada envio, na ordem dada. null = o envio
    // estoura (broker fora do ar).
    private void brokerAnswers(Boolean... answers) {
        int[] call = {0};
        doAnswer(invocation -> {
            Boolean answer = answers[call[0]++];
            if (answer == null) {
                throw new AmqpConnectException(new java.net.ConnectException("Connection refused"));
            }
            sentRoutingKeys.add(invocation.getArgument(1));
            sentMessages.add(invocation.getArgument(2));
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(answer, answer ? null : "nack"));
            return null;
        }).when(rabbitTemplate).send(eq(Messaging.EVENTS_EXCHANGE), any(String.class), any(Message.class),
                any(CorrelationData.class));
    }

    @Test
    void confirmacaoDoBrokerMarcaComoPublicado() {
        OutboxEvent created = event("e1", "review.created");
        when(repository.findTop100ByPublishedAtIsNullOrderByIdAsc()).thenReturn(List.of(created));
        brokerAnswers(true);

        relay.relayPending();

        assertThat(created.getPublishedAt()).isEqualTo(NOW);
        assertThat(created.getLastError()).isNull();
        verify(repository).save(created);
    }

    @Test
    void aRoutingKeyEOTipoDoEventoEOsMetadadosVaoNaMensagem() {
        when(repository.findTop100ByPublishedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(event("e1", "collection.updated")));
        brokerAnswers(true);

        relay.relayPending();

        assertThat(sentRoutingKeys).containsExactly("collection.updated");
        Message message = sentMessages.get(0);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("e1");
        assertThat(message.getMessageProperties().getType()).isEqualTo("collection.updated");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(message.getBody())).isEqualTo("{\"eventId\":\"e1\"}");
    }

    @Test
    void nackDoBrokerDeixaPendenteComOErro() {
        OutboxEvent created = event("e1", "review.created");
        when(repository.findTop100ByPublishedAtIsNullOrderByIdAsc()).thenReturn(List.of(created));
        brokerAnswers(false);

        relay.relayPending();

        assertThat(created.getPublishedAt()).isNull();
        assertThat(created.getAttempts()).isEqualTo(1);
        assertThat(created.getLastError()).contains("nack");
    }

    @Test
    void paraNoPrimeiroQueFalhouParaPreservarAOrdem() {
        OutboxEvent first = event("e1", "review.created");
        OutboxEvent second = event("e2", "review.deleted");
        OutboxEvent third = event("e3", "review.created");
        when(repository.findTop100ByPublishedAtIsNullOrderByIdAsc()).thenReturn(List.of(first, second, third));
        brokerAnswers(true, null, true);

        relay.relayPending();

        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNull();
        assertThat(second.getLastError()).contains("Connection refused");
        // O terceiro nem foi tentado: sairia antes do segundo.
        assertThat(third.getAttempts()).isZero();
        verify(repository, never()).save(third);
    }

    @Test
    void semConfirmacaoDentroDoPrazoContaComoFalha() {
        OutboxEvent created = event("e1", "review.created");
        when(repository.findTop100ByPublishedAtIsNullOrderByIdAsc()).thenReturn(List.of(created));
        // Envia mas o broker nunca confirma.
        doAnswer(invocation -> null).when(rabbitTemplate).send(any(String.class), any(String.class),
                any(Message.class), any(CorrelationData.class));

        relay = new OutboxRelay(repository, rabbitTemplate, Clock.fixed(NOW, ZoneOffset.UTC),
                emptyProvider(), 50);
        relay.relayPending();

        assertThat(created.getPublishedAt()).isNull();
        assertThat(created.getLastError()).contains("Timeout");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
