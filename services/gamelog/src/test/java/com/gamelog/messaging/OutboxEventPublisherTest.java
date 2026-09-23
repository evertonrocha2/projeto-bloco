package com.gamelog.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gamelog.messaging.event.UserRegisteredPayload;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// O lado "escrever" do outbox: o que vai pra tabela e, principalmente, QUANDO.
//
// O teste que importa e o da transacao desfeita. E ele que prova a promessa do
// padrao: se a operacao de negocio nao se confirmou, o evento tambem nao existe.
// Com um rabbitTemplate.send() direto no service, a mensagem ja teria saido.
@DataJpaTest
@Import({OutboxEventPublisher.class, OutboxEventPublisherTest.Config.class})
class OutboxEventPublisherTest {

    static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        ObjectMapper objectMapper() {
            // Mesmo formato de data do ObjectMapper do Spring Boot (ISO-8601).
            return new ObjectMapper().registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    @Autowired
    private OutboxEventPublisher publisher;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // Os testes sem transacao de teste commitam de verdade; limpa pra nao vazar.
    @AfterEach
    void cleanUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void gravaOEnvelopeCompletoNaTabela() throws Exception {
        publisher.publish(EventTypes.USER_REGISTERED, "ana", new UserRegisteredPayload("ana"));

        OutboxEvent saved = outboxRepository.findAll().get(0);
        assertThat(saved.getEventType()).isEqualTo("user.registered");
        assertThat(saved.getAggregateId()).isEqualTo("ana");
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getOccurredAt()).isEqualTo(NOW);

        // O JSON gravado E a mensagem: o relay manda estes bytes sem mexer.
        JsonNode envelope = objectMapper.readTree(saved.getPayload());
        assertThat(envelope.get("eventId").asText()).isEqualTo(saved.getEventId());
        assertThat(envelope.get("eventType").asText()).isEqualTo("user.registered");
        assertThat(envelope.get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("source").asText()).isEqualTo("gamelog");
        assertThat(envelope.get("occurredAt").asText()).isEqualTo("2026-09-20T12:00:00Z");
        assertThat(envelope.at("/payload/username").asText()).isEqualTo("ana");
    }

    @Test
    void cadaEventoGanhaUmIdProprio() {
        publisher.publish(EventTypes.USER_REGISTERED, "ana", new UserRegisteredPayload("ana"));
        publisher.publish(EventTypes.USER_REGISTERED, "beto", new UserRegisteredPayload("beto"));

        assertThat(outboxRepository.findAll())
                .extracting(OutboxEvent::getEventId)
                .doesNotHaveDuplicates()
                .hasSize(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transacaoDesfeitaNaoDeixaEvento() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            publisher.publish(EventTypes.USER_REGISTERED, "ana", new UserRegisteredPayload("ana"));
            // O que quer que falhe depois (constraint, regra de negocio) desfaz tudo.
            throw new IllegalStateException("falhou depois de publicar");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transacaoConfirmadaDeixaEventoPendente() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                publisher.publish(EventTypes.USER_REGISTERED, "ana", new UserRegisteredPayload("ana")));

        assertThat(outboxRepository.countByPublishedAtIsNull()).isEqualTo(1);
    }
}
