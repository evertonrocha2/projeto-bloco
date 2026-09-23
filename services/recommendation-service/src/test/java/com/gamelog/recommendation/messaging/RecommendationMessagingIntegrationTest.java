package com.gamelog.recommendation.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.gamelog.recommendation.dto.RecommendationItem;
import com.gamelog.recommendation.projection.ProcessedEventRepository;
import com.gamelog.recommendation.projection.UserRatingViewRepository;
import com.gamelog.recommendation.service.RecommendationService;
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

// O consumidor de verdade contra um RabbitMQ de verdade.
//
// Aqui o teste faz o papel do monolito: publica no exchange gamelog.events os
// mesmos envelopes JSON que o OutboxRelay publica. Isso prova o contrato do lado
// do consumidor sem subir o monolito - e e exatamente o desacoplamento que a
// arquitetura promete: pra este servico, "o GameLog" e um formato de mensagem.
@SpringBootTest(properties = {
        "app.snapshot.bootstrap.enabled=false",
        "app.messaging.retry.initial-interval-ms=50",
        "spring.datasource.url=jdbc:h2:mem:recommendation-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
class RecommendationMessagingIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRatingViewRepository ratingRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private String publish(String type, int schemaVersion, Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", type);
        envelope.put("schemaVersion", schemaVersion);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("source", "gamelog");
        envelope.put("aggregateId", "1");
        envelope.put("payload", payload);
        rabbitTemplate.convertAndSend(Messaging.EVENTS_EXCHANGE, type, envelope);
        return eventId;
    }

    private static Map<String, Object> game(long id, String title, String genre) {
        return Map.of("gameId", id, "title", title, "genre", genre, "coverUrl", "capa-" + id);
    }

    private static Map<String, Object> review(String user, long gameId, String genre, int rating) {
        return Map.of("reviewId", gameId, "username", user, "gameId", gameId,
                "gameTitle", "Jogo " + gameId, "genre", genre, "rating", rating);
    }

    @Test
    void eventosAlimentamAProjecaoEJogoNovoDisparaRecalculo() {
        publish("catalog.game.added", 1, game(1, "Elden Ring", "Action, RPG"));
        publish("catalog.game.added", 1, game(2, "FIFA", "Sports"));
        String lastEvent = publish("review.created", 1, review("ana", 99, "RPG", 5));

        await().atMost(Duration.ofSeconds(15)).until(() -> processedEventRepository.existsById(lastEvent));

        // Primeiro acesso: calcula com o que a projecao ja sabe. Nenhuma chamada HTTP.
        assertThat(recommendationService.getRecommendations("ana").items())
                .extracting(RecommendationItem::gameTitle)
                .contains("Elden Ring")
                .doesNotContain("Jogo 99");

        // Jogo novo no catalogo: o servico manda um RecalculateCommand pra cada
        // usuario com lote, e a fila de trabalho recalcula sem ninguem clicar.
        publish("catalog.game.added", 1, game(3, "Baldur's Gate 3", "RPG"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(recommendationService.getRecommendations("ana").items())
                        .extracting(RecommendationItem::gameTitle)
                        .contains("Baldur's Gate 3"));
    }

    @Test
    void mensagemVenenosaVaiPraDlqENaoTravaAFila() {
        publish("review.created", 99, review("beto", 10, "RPG", 5));
        String valid = publish("review.created", 1, review("beto", 11, "RPG", 4));

        Message dead = rabbitTemplate.receive(Messaging.ACTIVITY_DLQ, 15_000);
        assertThat(dead).isNotNull();
        assertThat(new String(dead.getBody())).contains("\"schemaVersion\":99");
        // O RabbitMQ anota de onde e por que a mensagem morreu.
        assertThat(dead.getMessageProperties().getXDeathHeader()).isNotEmpty();

        // A mensagem valida atras dela foi processada normalmente.
        await().atMost(Duration.ofSeconds(15)).until(() -> processedEventRepository.existsById(valid));
        assertThat(ratingRepository.findByUsernameAndGameId("beto", 10L)).isEmpty();
        assertThat(ratingRepository.findByUsernameAndGameId("beto", 11L)).isPresent();
    }

    @Test
    void reentregaDoMesmoEventoNaoDuplica() {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", "review.created");
        envelope.put("schemaVersion", 1);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("source", "gamelog");
        envelope.put("aggregateId", "1");
        envelope.put("payload", review("carla", 20, "RPG", 3));

        rabbitTemplate.convertAndSend(Messaging.EVENTS_EXCHANGE, "review.created", envelope);
        rabbitTemplate.convertAndSend(Messaging.EVENTS_EXCHANGE, "review.created", envelope);
        String marker = publish("review.created", 1, review("carla", 21, "RPG", 2));

        await().atMost(Duration.ofSeconds(15)).until(() -> processedEventRepository.existsById(marker));
        assertThat(ratingRepository.findByUsernameAndDeletedFalse("carla")).hasSize(2);
    }
}
