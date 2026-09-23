package com.gamelog.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.integration.snapshot.CatalogSnapshot;
import com.gamelog.integration.snapshot.SnapshotRequest;
import com.gamelog.review.dto.CreateReviewRequest;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.review.service.ReviewService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Monolito de verdade + RabbitMQ de verdade (em container).
//
// Prova, ponta a ponta dentro do produtor:
//  - operacao de negocio -> linha no outbox -> relay -> mensagem no exchange,
//    com a routing key e o envelope que os consumidores esperam;
//  - a ordem de publicacao segue a ordem das operacoes;
//  - o request/reply de snapshot responde com o estado atual.
//
// Um "espiao" (fila temporaria ligada ao exchange com "#") faz o papel de um
// assinante qualquer - exatamente como os servicos reais assinam.
@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.outbox.relay.interval-ms=100",
        "spring.datasource.url=jdbc:h2:mem:gamelog-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.uploads.dir=target/test-uploads"
})
@Testcontainers
class GameLogMessagingIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    private static final String SPY_QUEUE = "test.spy.gamelog-events";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Game zelda;

    @BeforeEach
    void setUp() {
        // Sem auto-delete: o receive() com timeout cria e cancela um consumidor, e
        // uma fila auto-delete sumiria no primeiro cancelamento.
        Queue spy = new Queue(SPY_QUEUE, false, false, false);
        amqpAdmin.declareQueue(spy);
        amqpAdmin.declareBinding(BindingBuilder.bind(spy)
                .to(new TopicExchange(Messaging.EVENTS_EXCHANGE)).with("#"));
        amqpAdmin.purgeQueue(SPY_QUEUE, false);

        userRepository.save(new User("ana", "ana@email.com", "hash", null));
        zelda = gameRepository.save(new Game(9001L, "Zelda", null, 2017, "Adventure, RPG", "url"));
    }

    // Espera o relay esvaziar o outbox antes de limpar: um evento deste teste
    // chegando no espiao do proximo embaralharia as asercoes de ordem.
    @AfterEach
    void tearDown() {
        await().atMost(Duration.ofSeconds(10)).until(() -> outboxRepository.countByPublishedAtIsNull() == 0);
        amqpAdmin.deleteQueue(SPY_QUEUE);
        reviewRepository.deleteAll();
        gameRepository.deleteAll();
        userRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    private List<JsonNode> receive(int count) {
        List<JsonNode> received = new ArrayList<>();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            Message message = rabbitTemplate.receive(SPY_QUEUE, 200);
            if (message != null) {
                received.add(objectMapper.readTree(message.getBody()));
            }
            return received.size() >= count;
        });
        return received;
    }

    @Test
    void operacaoDeNegocioViraEventoNoBroker() {
        ReviewResponse review = reviewService.create("ana", zelda.getId(), new CreateReviewRequest(5, "obra prima"));

        JsonNode event = receive(1).get(0);
        assertThat(event.get("eventType").asText()).isEqualTo("review.created");
        assertThat(event.get("aggregateId").asText()).isEqualTo(String.valueOf(review.id()));
        assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(event.at("/payload/username").asText()).isEqualTo("ana");
        assertThat(event.at("/payload/gameId").asLong()).isEqualTo(zelda.getId());
        assertThat(event.at("/payload/genre").asText()).isEqualTo("Adventure, RPG");
        assertThat(event.at("/payload/rating").asInt()).isEqualTo(5);

        // E o outbox fica marcado como entregue depois da confirmacao do broker.
        await().atMost(Duration.ofSeconds(5)).until(() -> outboxRepository.countByPublishedAtIsNull() == 0);
    }

    @Test
    void eventosSaemNaOrdemDasOperacoes() {
        ReviewResponse review = reviewService.create("ana", zelda.getId(), new CreateReviewRequest(3, "ok"));
        reviewService.update("ana", review.id(), new CreateReviewRequest(4, "melhorou"));
        reviewService.delete("ana", review.id());

        List<JsonNode> events = receive(3);
        assertThat(events).extracting(e -> e.get("eventType").asText())
                .containsExactly("review.created", "review.updated", "review.deleted");
        // O delete carrega o ultimo estado: o consumidor sabe qual nota tirar.
        assertThat(events.get(2).at("/payload/rating").asInt()).isEqualTo(4);
    }

    @Test
    void snapshotRespondidoPorRequestReply() {
        reviewService.create("ana", zelda.getId(), new CreateReviewRequest(5, "obra prima"));

        CatalogSnapshot snapshot = rabbitTemplate.convertSendAndReceiveAsType(
                "", Messaging.SNAPSHOT_REQUEST_QUEUE, new SnapshotRequest("teste"),
                new ParameterizedTypeReference<CatalogSnapshot>() {
                });

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.games()).extracting(g -> g.title()).contains("Zelda");
        assertThat(snapshot.ratings()).anySatisfy(r -> {
            assertThat(r.username()).isEqualTo("ana");
            assertThat(r.rating()).isEqualTo(5);
        });
    }
}
