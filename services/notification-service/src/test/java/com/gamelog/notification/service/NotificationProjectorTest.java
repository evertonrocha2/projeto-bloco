package com.gamelog.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gamelog.notification.domain.Notification;
import com.gamelog.notification.domain.NotificationType;
import com.gamelog.notification.messaging.GameLogEvent;
import com.gamelog.notification.repository.FeedItemRepository;
import com.gamelog.notification.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// As regras de quem e avisado de que. Moram neste servico, e nao no monolito:
// o evento conta quem esta envolvido, e o consumidor decide o que fazer com isso.
@DataJpaTest
@Import({NotificationProjector.class, NotificationProjectorTest.Config.class})
class NotificationProjectorTest {

    static final Instant T0 = Instant.parse("2026-09-20T12:00:00Z");

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.fixed(T0, ZoneOffset.UTC);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    @Autowired
    private NotificationProjector projector;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private GameLogEvent event(String type, Map<String, Object> payload) {
        return event(UUID.randomUUID().toString(), type, 1, payload);
    }

    private GameLogEvent event(String id, String type, int version, Map<String, Object> payload) {
        return new GameLogEvent(id, type, version, T0, "gamelog", "1", objectMapper.valueToTree(payload));
    }

    private static Map<String, Object> reply(String reviewAuthor, String replyAuthor, String parentAuthor) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reviewId", 7);
        payload.put("replyId", 70);
        payload.put("reviewAuthor", reviewAuthor);
        payload.put("replyAuthor", replyAuthor);
        payload.put("parentAuthor", parentAuthor);
        payload.put("gameId", 1);
        payload.put("gameTitle", "Zelda");
        payload.put("excerpt", "discordo do final");
        return payload;
    }

    private static Map<String, Object> vote(String author, String voter, String type) {
        return Map.of("reviewId", 7, "reviewAuthor", author, "voter", voter, "voteType", type,
                "gameId", 1, "gameTitle", "Zelda");
    }

    @Test
    void cadastroGeraBoasVindas() {
        projector.apply(event("user.registered", Map.of("username", "ana")));

        assertThat(notificationRepository.findAll()).singleElement()
                .satisfies(n -> {
                    assertThat(n.getRecipient()).isEqualTo("ana");
                    assertThat(n.getType()).isEqualTo(NotificationType.WELCOME);
                });
    }

    @Test
    void respostaDiretaAvisaODonoDaAvaliacao() {
        int created = projector.apply(event("review.replied", reply("ana", "beto", null)));

        assertThat(created).isEqualTo(1);
        Notification n = notificationRepository.findAll().get(0);
        assertThat(n.getRecipient()).isEqualTo("ana");
        assertThat(n.getType()).isEqualTo(NotificationType.REVIEW_REPLY);
        assertThat(n.getMessage()).contains("beto").contains("Zelda").contains("discordo do final");
    }

    @Test
    void respostaEmThreadAvisaOsDois() {
        projector.apply(event("review.replied", reply("ana", "carla", "beto")));

        assertThat(notificationRepository.findAll())
                .extracting(Notification::getRecipient, Notification::getType)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ana", NotificationType.REVIEW_REPLY),
                        org.assertj.core.groups.Tuple.tuple("beto", NotificationType.THREAD_REPLY));
    }

    @Test
    void ninguemEAvisadoDaPropriaAcao() {
        // ana responde na propria avaliacao, a um comentario do beto.
        projector.apply(event("review.replied", reply("ana", "ana", "beto")));

        assertThat(notificationRepository.findAll())
                .extracting(Notification::getRecipient).containsExactly("beto");
    }

    @Test
    void donoDaAvaliacaoNaoRecebeAvisoDobrado() {
        // beto responde a um comentario da propria ana, na avaliacao da ana.
        projector.apply(event("review.replied", reply("ana", "beto", "ana")));

        assertThat(notificationRepository.findAll()).hasSize(1);
    }

    @Test
    void soVotoPositivoViraAviso() {
        projector.apply(event("review.voted", vote("ana", "beto", "POSITIVE")));
        projector.apply(event("review.voted", vote("ana", "carla", "NEGATIVE")));

        assertThat(notificationRepository.findAll())
                .extracting(Notification::getActor).containsExactly("beto");
    }

    @Test
    void eventoRepetidoNaoAvisaDuasVezes() {
        GameLogEvent replied = event("e-1", "review.replied", 1, reply("ana", "beto", null));

        projector.apply(replied);
        int second = projector.apply(replied);

        assertThat(second).isZero();
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void feedRecebeReviewPublicadaEPerdeAApagada() {
        Map<String, Object> review = Map.of("reviewId", 7, "username", "ana", "gameId", 1,
                "gameTitle", "Zelda", "rating", 5);

        projector.apply(event("review.created", review));
        assertThat(feedItemRepository.findById(7L)).isPresent();

        projector.apply(event("review.deleted", review));
        assertThat(feedItemRepository.findById(7L)).isEmpty();
    }

    @Test
    void versaoDesconhecidaERecusadaSemRetentativa() {
        assertThatThrownBy(() -> projector.apply(event("e-2", "user.registered", 2, Map.of("username", "ana"))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(notificationRepository.count()).isZero();
    }
}
