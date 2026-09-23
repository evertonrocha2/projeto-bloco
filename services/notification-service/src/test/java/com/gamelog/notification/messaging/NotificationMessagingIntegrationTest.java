package com.gamelog.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.gamelog.notification.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// O segundo assinante contra um RabbitMQ real.
//
// O teste mais importante aqui e o de fan-out: uma unica publicacao no exchange
// chega na fila deste servico E na fila de outro assinante qualquer, cada uma com
// a sua copia. E a propriedade que deixou este servico nascer sem mudar uma
// linha do monolito.
@SpringBootTest(properties = {
        "app.messaging.retry.initial-interval-ms=50",
        "spring.datasource.url=jdbc:h2:mem:notification-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
class NotificationMessagingIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    private String publish(String type, int version, Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", type);
        envelope.put("schemaVersion", version);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("source", "gamelog");
        envelope.put("aggregateId", "1");
        envelope.put("payload", payload);
        rabbitTemplate.convertAndSend(Messaging.EVENTS_EXCHANGE, type, envelope);
        return eventId;
    }

    @Test
    void mesmaPublicacaoChegaATodosOsAssinantes() {
        // Outro assinante (faz o papel do recommendation-service).
        String otherSubscriber = "test.other-subscriber";
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(otherSubscriber, false, false, false, null);
            channel.queueBind(otherSubscriber, Messaging.EVENTS_EXCHANGE, "user.*");
            return null;
        });

        publish("user.registered", 1, Map.of("username", "dora"));

        await().atMost(Duration.ofSeconds(15)).until(() ->
                notificationRepository.countByRecipientAndReadAtIsNull("dora") == 1);
        Message copy = rabbitTemplate.receive(otherSubscriber, 5_000);
        assertThat(copy).isNotNull();
        assertThat(new String(copy.getBody())).contains("\"username\":\"dora\"");
    }

    @Test
    void mensagemVenenosaVaiPraDlq() {
        publish("user.registered", 7, Map.of("username", "eva"));

        Message dead = rabbitTemplate.receive(Messaging.EVENTS_DLQ, 15_000);
        assertThat(dead).isNotNull();
        assertThat(notificationRepository.countByRecipientAndReadAtIsNull("eva")).isZero();
    }
}
