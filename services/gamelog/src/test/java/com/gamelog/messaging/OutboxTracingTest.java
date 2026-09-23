package com.gamelog.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gamelog.messaging.event.UserRegisteredPayload;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;

// TP5: o trace atravessa o outbox.
//
// Entre o POST que grava a review e a publicacao no RabbitMQ ha uma fronteira de
// thread e de tempo (o relay roda meio segundo depois, noutra thread). Sem cuidado
// explicito, o trace terminaria no commit. Estes testes provam as duas metades:
// o publisher guarda o contexto da requisicao; o relay reabre esse contexto em
// volta do envio, que e onde o RabbitTemplate escreve o header traceparent.
class OutboxTracingTest {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SimpleTracer tracer = new SimpleTracer();

    @SuppressWarnings("unchecked")
    private ObjectProvider<Tracer> provider(Tracer value) {
        ObjectProvider<Tracer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }

    @Test
    void publisherGuardaOTraceDaRequisicaoNaLinhaDoOutbox() {
        OutboxRepository repository = mock(OutboxRepository.class);
        OutboxEventPublisher publisher = new OutboxEventPublisher(repository,
                new ObjectMapper().registerModule(new JavaTimeModule()), clock, provider(tracer));

        Span request = tracer.nextSpan().name("POST /api/auth/register").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(request)) {
            publisher.publish(EventTypes.USER_REGISTERED, "ana", new UserRegisteredPayload("ana"));
        } finally {
            request.end();
        }

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTraceId()).isEqualTo(request.context().traceId());
        assertThat(saved.getValue().getSpanId()).isEqualTo(request.context().spanId());
    }

    @Test
    void semTraceAtivoOEventoSaiSemTrace() {
        OutboxRepository repository = mock(OutboxRepository.class);
        OutboxEventPublisher publisher = new OutboxEventPublisher(repository,
                new ObjectMapper().registerModule(new JavaTimeModule()), clock, provider(null));

        publisher.publish(EventTypes.USER_REGISTERED, "ana", new UserRegisteredPayload("ana"));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTraceId()).isNull();
    }

    @Test
    void relayPublicaDentroDeUmSpanFilhoDaRequisicaoOriginal() {
        OutboxEvent event = new OutboxEvent("e1", "review.created", "42", "{}", NOW);
        event.attachTrace("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331");

        OutboxRepository repository = mock(OutboxRepository.class);
        when(repository.findTop100ByPublishedAtIsNullOrderByIdAsc()).thenReturn(List.of(event));

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AtomicReference<Span> spanDuringSend = new AtomicReference<>();
        doAnswer(invocation -> {
            spanDuringSend.set(tracer.currentSpan());
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class),
                any(CorrelationData.class));

        @SuppressWarnings("unchecked")
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> noMetrics = mock(ObjectProvider.class);
        new OutboxRelay(repository, rabbitTemplate, clock, noMetrics, provider(tracer), 1000).relayPending();

        SimpleSpan relaySpan = tracer.onlySpan();
        assertThat(relaySpan.getTraceId()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(relaySpan.getParentId()).isEqualTo("b7ad6b7169203331");
        assertThat(relaySpan.getName()).isEqualTo("outbox relay review.created");
        assertThat(relaySpan.getTags()).containsEntry("event.id", "e1");
        assertThat(relaySpan.getEndTimestamp()).isNotNull();
        // O envio aconteceu COM o span em escopo: e isso que faz o RabbitTemplate
        // propagar o trace no header da mensagem.
        assertThat(spanDuringSend.get()).isNotNull();
        assertThat(spanDuringSend.get().context().traceId()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
    }
}
