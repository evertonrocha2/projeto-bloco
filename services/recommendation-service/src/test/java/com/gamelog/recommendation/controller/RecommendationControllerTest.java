package com.gamelog.recommendation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gamelog.recommendation.client.ActivitySource;
import com.gamelog.recommendation.client.GameLogSnapshot;
import com.gamelog.recommendation.config.ScoringProperties;
import com.gamelog.recommendation.domain.CatalogGame;
import com.gamelog.recommendation.domain.GameActivity;
import com.gamelog.recommendation.domain.RatedGame;
import com.gamelog.recommendation.domain.RecommendationEngine;
import com.gamelog.recommendation.repository.RecommendationFeedbackRepository;
import com.gamelog.recommendation.repository.RecommendationRepository;
import com.gamelog.recommendation.service.RecommendationService;
import com.gamelog.recommendation.shared.RecommendationExceptionHandler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Testa o contrato HTTP do microsservico: as rotas, os codigos de status e a
// forma do JSON.
//
// Isto e o que o front consome atraves do gateway, entao mudanca de nome de campo
// aqui quebra a tela. Montagem sem mock: repositorios reais, service real,
// algoritmo real, Jackson real - so o monolito e substituido por um duplo.
@DataJpaTest
class RecommendationControllerTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationFeedbackRepository feedbackRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GameActivity activity = new GameActivity("ana",
                List.of(new RatedGame(99L, "RPG", 5)),
                List.of());

        List<CatalogGame> catalog = List.of(
                new CatalogGame(1L, "Elden Ring", "url1", "Action, RPG", 4.5),
                new CatalogGame(2L, "FIFA", "url2", "Sports", 3.0));

        ActivitySource source = username ->
                Optional.of(new GameLogSnapshot(activity, catalog));

        RecommendationService service = new RecommendationService(
                recommendationRepository, feedbackRepository,
                new RecommendationEngine(), source, new ScoringProperties());

        mockMvc = MockMvcBuilders
                .standaloneSetup(new RecommendationController(service))
                .setControllerAdvice(new RecommendationExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devolve as recomendacoes com pontuacao e justificativa")
    void getReturnsRecommendationsWithScoreAndReason() throws Exception {
        mockMvc.perform(get("/api/recommendations/ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.items[0].gameId").value(1))
                .andExpect(jsonPath("$.items[0].gameTitle").value("Elden Ring"))
                .andExpect(jsonPath("$.items[0].score").exists())
                // A tela usa esta lista pra escrever "porque voce gosta de RPG".
                .andExpect(jsonPath("$.items[0].reasonGenres[0]").value("RPG"));
    }

    @Test
    @DisplayName("POST /refresh recalcula e devolve o lote novo")
    void refreshRecalculatesTheBatch() throws Exception {
        mockMvc.perform(post("/api/recommendations/ana/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("POST /feedback registra o veredito")
    void feedbackIsAccepted() throws Exception {
        mockMvc.perform(post("/api/recommendations/ana/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\": 1, \"verdict\": \"DISMISSED\"}"))
                .andExpect(status().isNoContent());

        // O efeito e observavel no banco proprio do servico.
        assertFeedbackWasStored();
    }

    private void assertFeedbackWasStored() {
        org.assertj.core.api.Assertions
                .assertThat(feedbackRepository.findByUsernameAndGameId("ana", 1L))
                .isPresent();
    }

    @Test
    @DisplayName("veredito invalido devolve 400 em vez de gravar lixo")
    void invalidVerdictIsRejected() throws Exception {
        // O verdict e um enum. Se a API aceitasse qualquer texto, o banco acabaria
        // com estados sem significado e o algoritmo nao saberia o que fazer com eles.
        mockMvc.perform(post("/api/recommendations/ana/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\": 1, \"verdict\": \"TALVEZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("feedback sem gameId devolve 400")
    void feedbackWithoutGameIdIsRejected() throws Exception {
        mockMvc.perform(post("/api/recommendations/ana/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\": \"LIKED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /taste-profile expoe o peso de cada genero")
    void tasteProfileExposesGenreWeights() throws Exception {
        mockMvc.perform(get("/api/recommendations/ana/taste-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.genres[0].genre").value("RPG"))
                .andExpect(jsonPath("$.genres[0].weight").value(1.0));
    }
}
