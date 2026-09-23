package com.gamelog.recommendation.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gamelog.recommendation.activity.GameLogSnapshot;
import com.gamelog.recommendation.activity.ProjectionActivitySource;
import com.gamelog.recommendation.messaging.GameLogEvent;
import com.gamelog.recommendation.messaging.SnapshotMessages.CatalogSnapshot;
import com.gamelog.recommendation.messaging.SnapshotMessages.Ownership;
import com.gamelog.recommendation.messaging.SnapshotMessages.Rating;
import com.gamelog.recommendation.messaging.EventPayloads.GameEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// A projecao local: o que torna o servico independente do monolito, e onde moram
// os problemas classicos de consumir eventos.
//
//  - duplicata (entrega "pelo menos uma vez");
//  - evento fora de ordem (retentativa, replay da DLQ);
//  - delete chegando antes do create;
//  - formato que o servico ainda nao conhece;
//  - snapshot inicial convivendo com eventos mais novos que ele.
@DataJpaTest
@Import({ActivityProjector.class, ProjectionActivitySource.class, ActivityProjectorTest.Config.class})
class ActivityProjectorTest {

    static final Instant T0 = Instant.parse("2026-09-20T12:00:00Z");

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.fixed(T0.plusSeconds(3600), ZoneOffset.UTC);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    @Autowired
    private ActivityProjector projector;

    @Autowired
    private ProjectionActivitySource activitySource;

    @Autowired
    private UserRatingViewRepository ratingRepository;

    @Autowired
    private CatalogGameViewRepository catalogRepository;

    @Autowired
    private UserCollectionViewRepository collectionRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private GameLogEvent event(String type, Instant at, Map<String, Object> payload) {
        return event(UUID.randomUUID().toString(), type, 1, at, payload);
    }

    private GameLogEvent event(String id, String type, int version, Instant at, Map<String, Object> payload) {
        return new GameLogEvent(id, type, version, at, "gamelog", "1", objectMapper.valueToTree(payload));
    }

    private static Map<String, Object> review(String user, long gameId, int rating) {
        return Map.of("reviewId", 1, "username", user, "gameId", gameId,
                "gameTitle", "Jogo " + gameId, "genre", "RPG", "rating", rating);
    }

    private int ratingOf(String user, long gameId) {
        return ratingRepository.findByUsernameAndGameId(user, gameId).orElseThrow().getRating();
    }

    @Test
    void reviewCriadaViraNotaEPedeRecalculoDoAutor() {
        ProjectionOutcome outcome = projector.apply(event("review.created", T0, review("ana", 10, 5)));

        assertThat(ratingOf("ana", 10)).isEqualTo(5);
        assertThat(outcome.affectedUsers()).containsExactly("ana");
        // O jogo que o evento cita entra no catalogo local mesmo sem catalog.game.added.
        assertThat(catalogRepository.findById(10L)).isPresent();
    }

    @Test
    void mesmoEventoEntregueDuasVezesAplicaUmaSo() {
        GameLogEvent created = event("e-1", "review.created", 1, T0, review("ana", 10, 5));

        projector.apply(created);
        ProjectionOutcome second = projector.apply(created);

        assertThat(second.duplicate()).isTrue();
        assertThat(ratingRepository.findAll()).hasSize(1);
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void eventoAtrasadoNaoSobrescreveEstadoMaisNovo() {
        projector.apply(event("review.updated", T0.plusSeconds(10), review("ana", 10, 2)));
        // Chega depois, mas aconteceu antes (ex.: saiu da DLQ num replay).
        ProjectionOutcome late = projector.apply(event("review.created", T0, review("ana", 10, 5)));

        assertThat(ratingOf("ana", 10)).isEqualTo(2);
        assertThat(late.affectedUsers()).isEmpty();
    }

    @Test
    void deleteAntesDoCreateDeixaLapideQueBarraOCreateAtrasado() {
        projector.apply(event("review.deleted", T0.plusSeconds(10), review("ana", 10, 5)));
        projector.apply(event("review.created", T0, review("ana", 10, 5)));

        assertThat(ratingRepository.findByUsernameAndDeletedFalse("ana")).isEmpty();
    }

    @Test
    void recriarDepoisDeApagarReativaANota() {
        projector.apply(event("review.created", T0, review("ana", 10, 5)));
        projector.apply(event("review.deleted", T0.plusSeconds(10), review("ana", 10, 5)));
        projector.apply(event("review.created", T0.plusSeconds(20), review("ana", 10, 3)));

        assertThat(ratingRepository.findByUsernameAndDeletedFalse("ana"))
                .extracting(UserRatingView::getRating).containsExactly(3);
    }

    @Test
    void jogoNovoNoCatalogoMarcaMudancaDeCatalogo() {
        ProjectionOutcome outcome = projector.apply(event("catalog.game.added", T0,
                Map.of("gameId", 50, "title", "Hades", "genre", "Roguelike", "coverUrl", "capa")));

        assertThat(outcome.catalogChanged()).isTrue();
        assertThat(catalogRepository.findById(50L)).get()
                .extracting(CatalogGameView::getTitle).isEqualTo("Hades");
    }

    @Test
    void versaoDeSchemaDesconhecidaVaiPraDlqSemTocarNaProjecao() {
        GameLogEvent future = event("e-9", "review.created", 2, T0, review("ana", 10, 5));

        assertThatThrownBy(() -> projector.apply(future))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(ratingRepository.count()).isZero();
        assertThat(processedEventRepository.existsById("e-9")).isFalse();
    }

    @Test
    void payloadQuebradoERecusadoSemRetentativa() {
        GameLogEvent broken = new GameLogEvent("e-x", "review.created", 1, T0, "gamelog", "1", null);

        assertThatThrownBy(() -> projector.apply(broken))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void tipoQueNaoInteressaEConfirmadoEIgnorado() {
        ProjectionOutcome outcome = projector.apply(event("review.voted", T0, Map.of("reviewId", 1)));

        assertThat(outcome.affectedUsers()).isEmpty();
        assertThat(outcome.catalogChanged()).isFalse();
    }

    @Test
    void snapshotPreencheAProjecaoMasNaoAtropelaEventoMaisNovo() {
        // Evento mais novo que a foto: ana mudou a nota do jogo 10 para 1.
        projector.apply(event("review.updated", T0.plusSeconds(60), review("ana", 10, 1)));
        // Nota antiga que o snapshot nao tem mais: foi apagada enquanto o servico nao ouvia.
        ratingRepository.save(new UserRatingView("beto", 11L, 4, T0.minusSeconds(60)));

        projector.applySnapshot(new CatalogSnapshot(T0,
                List.of(new GameEvent(10L, "Elden Ring", "Action, RPG", "c10"),
                        new GameEvent(11L, "FIFA", "Sports", "c11")),
                List.of(new Rating("ana", 10L, 5), new Rating("carla", 11L, 3)),
                List.of(new Ownership("carla", 10L, "PLAYING"))));

        assertThat(ratingOf("ana", 10)).isEqualTo(1);
        assertThat(ratingOf("carla", 11)).isEqualTo(3);
        assertThat(ratingRepository.findByUsernameAndDeletedFalse("beto")).isEmpty();
        assertThat(collectionRepository.findByUsername("carla")).hasSize(1);
        assertThat(catalogRepository.findById(10L)).get()
                .extracting(CatalogGameView::getCoverUrl).isEqualTo("c10");
    }

    @Test
    void retratoMontadoDaProjecaoTemMediaDaComunidadeEGeneroDoCatalogo() {
        projector.apply(event("catalog.game.added", T0,
                Map.of("gameId", 10, "title", "Elden Ring", "genre", "Action, RPG", "coverUrl", "c")));
        projector.apply(event("review.created", T0, review("ana", 10, 5)));
        projector.apply(event("review.created", T0, review("beto", 10, 2)));

        GameLogSnapshot snapshot = activitySource.fetch("ana").orElseThrow();

        assertThat(snapshot.catalog()).singleElement()
                .satisfies(game -> assertThat(game.averageRating()).isEqualTo(3.5));
        assertThat(snapshot.activity().ratedGames()).singleElement()
                .satisfies(rated -> assertThat(rated.genre()).isEqualTo("Action, RPG"));
    }

    @Test
    void projecaoVaziaSemSnapshotNaoTemRetrato() {
        assertThat(activitySource.fetch("ana")).isEmpty();
    }
}
