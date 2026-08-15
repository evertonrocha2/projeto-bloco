package com.gamelog.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.recommendation.client.ActivitySource;
import com.gamelog.recommendation.client.GameLogSnapshot;
import com.gamelog.recommendation.config.ScoringProperties;
import com.gamelog.recommendation.domain.CatalogGame;
import com.gamelog.recommendation.domain.FeedbackVerdict;
import com.gamelog.recommendation.domain.GameActivity;
import com.gamelog.recommendation.domain.RatedGame;
import com.gamelog.recommendation.domain.RecommendationEngine;
import com.gamelog.recommendation.dto.RecommendationsResponse;
import com.gamelog.recommendation.repository.RecommendationFeedbackRepository;
import com.gamelog.recommendation.repository.RecommendationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// Testa o servico com repositorios REAIS (@DataJpaTest sobre H2) e um duplo
// escrito a mao no lugar do monolito.
//
// O duplo e uma classe de dez linhas, nao um mock de framework, e isso e
// deliberado: o que precisa ser controlado no teste e apenas "o monolito
// responde ou nao responde". Um mock com expectativas de chamada acabaria
// afirmando o comportamento do proprio mock; aqui as afirmacoes sao todas sobre
// o que o servico gravou no banco e devolveu.
//
// O ActivitySource existir como interface e o que torna isso possivel - e o
// mesmo motivo pelo qual os services do monolito dependem de interfaces de
// repositorio, e nao de implementacoes.
@DataJpaTest
class RecommendationServiceTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationFeedbackRepository feedbackRepository;

    private FakeGameLog gameLog;
    private RecommendationService service;

    // Faz o papel do monolito. snapshot nulo = servico fora do ar.
    private static class FakeGameLog implements ActivitySource {
        private GameLogSnapshot snapshot;
        private int calls;

        FakeGameLog(GameLogSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Optional<GameLogSnapshot> fetch(String username) {
            calls++;
            return Optional.ofNullable(snapshot);
        }

        void goOffline() {
            snapshot = null;
        }
    }

    @BeforeEach
    void setUp() {
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(99L, "RPG", 5)),
                List.of());

        List<CatalogGame> catalog = List.of(
                new CatalogGame(1L, "Elden Ring", "url1", "Action, RPG", 4.5),
                new CatalogGame(2L, "FIFA", "url2", "Sports", 3.0),
                new CatalogGame(3L, "Hades", "url3", "Roguelike", 4.8));

        gameLog = new FakeGameLog(new GameLogSnapshot(activity, catalog));

        ScoringProperties scoring = new ScoringProperties();
        scoring.setMinRating(3);
        scoring.setCollectionWeight(0.5);
        scoring.setLikedBoost(1.5);
        scoring.setGenreWeight(3.0);
        scoring.setCommunityWeight(2.0);
        scoring.setMaxResults(8);

        service = new RecommendationService(
                recommendationRepository, feedbackRepository,
                new RecommendationEngine(), gameLog, scoring);
    }

    @Test
    @DisplayName("gera e persiste as recomendacoes na primeira consulta")
    void generatesAndPersistsOnFirstRequest() {
        RecommendationsResponse response = service.getRecommendations("ana");

        assertThat(response.stale()).isFalse();
        assertThat(response.items()).isNotEmpty();
        // Elden Ring casa com o gosto por RPG, entao lidera.
        assertThat(response.items().get(0).gameId()).isEqualTo(1L);
        // E o lote ficou gravado no banco proprio do microsservico.
        assertThat(recommendationRepository.findByUsernameOrderByScoreDesc("ana")).isNotEmpty();
    }

    @Test
    @DisplayName("serve o lote gravado sem chamar o monolito de novo")
    void servesStoredBatchWithoutCallingTheMonolithAgain() {
        service.getRecommendations("ana");
        int chamadasDepoisDaPrimeira = gameLog.calls;

        service.getRecommendations("ana");

        // Abrir a tela nao pode custar uma chamada de rede a cada vez. Recalcular e
        // uma acao explicita do usuario.
        assertThat(gameLog.calls).isEqualTo(chamadasDepoisDaPrimeira);
    }

    @Test
    @DisplayName("monolito fora do ar: serve o lote anterior marcado como desatualizado")
    void monolithDownServesPreviousBatchFlaggedStale() {
        // Este e o comportamento central de resiliencia da entrega: o microsservico
        // continua util quando a dependencia dele cai, porque tem banco proprio.
        service.getRecommendations("ana");
        gameLog.goOffline();

        RecommendationsResponse response = service.refresh("ana");

        assertThat(response.items()).isNotEmpty();
        assertThat(response.stale()).isTrue();
    }

    @Test
    @DisplayName("monolito fora do ar e sem lote anterior: lista vazia, nao erro")
    void monolithDownWithoutPreviousBatchReturnsEmptyNotError() {
        gameLog.goOffline();

        RecommendationsResponse response = service.getRecommendations("novato");

        assertThat(response.items()).isEmpty();
        assertThat(response.stale()).isTrue();
        // 200 com lista vazia e stale=true deixa a tela explicar a situacao. Um 500
        // faria o front mostrar "algo deu errado", que nao ajuda ninguem.
    }

    @Test
    @DisplayName("recalcular substitui o lote sem duplicar")
    void refreshReplacesTheBatch() {
        service.getRecommendations("ana");
        int antes = recommendationRepository.findByUsernameOrderByScoreDesc("ana").size();

        service.refresh("ana");

        assertThat(recommendationRepository.findByUsernameOrderByScoreDesc("ana"))
                .hasSize(antes);
    }

    @Test
    @DisplayName("descartar um jogo tira ele do lote atual na hora")
    void dismissingRemovesTheGameFromTheCurrentBatch() {
        service.getRecommendations("ana");

        service.registerFeedback("ana", 1L, FeedbackVerdict.DISMISSED);

        assertThat(recommendationRepository.findByUsernameOrderByScoreDesc("ana"))
                .noneMatch(rec -> rec.getGameId().equals(1L));
    }

    @Test
    @DisplayName("jogo descartado nao volta no recalculo seguinte")
    void dismissedGameDoesNotComeBackOnRefresh() {
        // Se voltasse, o "nao me interessa" nao teria efeito nenhum e o usuario
        // descartaria o mesmo jogo pra sempre.
        service.getRecommendations("ana");
        service.registerFeedback("ana", 1L, FeedbackVerdict.DISMISSED);

        RecommendationsResponse response = service.refresh("ana");

        assertThat(response.items()).noneMatch(item -> item.gameId().equals(1L));
    }

    @Test
    @DisplayName("mudar de opiniao atualiza o veredito em vez de criar outro")
    void changingOpinionUpdatesTheVerdict() {
        service.registerFeedback("ana", 1L, FeedbackVerdict.DISMISSED);
        service.registerFeedback("ana", 1L, FeedbackVerdict.LIKED);

        assertThat(feedbackRepository.findByUsername("ana")).hasSize(1);
        assertThat(feedbackRepository.findByUsernameAndGameId("ana", 1L))
                .get()
                .extracting(feedback -> feedback.getVerdict())
                .isEqualTo(FeedbackVerdict.LIKED);
    }

    @Test
    @DisplayName("o perfil de gosto expoe o peso de cada genero")
    void tasteProfileExposesWeightPerGenre() {
        // E o que torna a recomendacao explicavel na tela: o usuario ve que o
        // sistema entendeu que ele gosta de RPG.
        var profile = service.getTasteProfile("ana");

        assertThat(profile.genres()).isNotEmpty();
        assertThat(profile.genres().get(0).genre()).isEqualTo("RPG");
        assertThat(profile.genres().get(0).weight()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("perfil de gosto vazio quando o monolito nao responde")
    void tasteProfileIsEmptyWhenMonolithIsUnreachable() {
        gameLog.goOffline();

        var profile = service.getTasteProfile("ana");

        assertThat(profile.genres()).isEmpty();
    }
}
