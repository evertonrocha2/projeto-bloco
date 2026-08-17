package com.gamelog.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.collection.domain.CollectionStatus;
import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.integration.controller.GameActivityController;
import com.gamelog.integration.service.GameActivityService;
import com.gamelog.review.domain.Review;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.shared.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Testa o endpoint que o microsservico de recomendacoes consome.
//
// O JSON daqui e um CONTRATO entre dois processos: se um nome de campo mudar, o
// microsservico para de entender a resposta e ninguem descobre em tempo de
// compilacao - os dois lados compilam sozinhos. Por isso o teste afirma os
// caminhos do JSON, e nao so o objeto Java.
//
// Montagem sem mock nenhum: @DataJpaTest da repositorios de verdade sobre um H2
// de verdade, o service e o controller sao instanciados na mao, e o
// standaloneSetup passa a resposta pelo Jackson real. O GlobalExceptionHandler
// entra junto porque o 404 tambem faz parte do contrato.
@DataJpaTest
class GameActivityEndpointTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private GameRepository gameRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        userRepository.save(new User("novato", "novato@email.com", "hash", null));

        Game zelda = gameRepository.save(new Game(401L, "Zelda", null, 2017, "Aventura", "url"));
        Game hades = gameRepository.save(new Game(402L, "Hades", null, 2020, "Roguelike, Indie", "url"));

        reviewRepository.saveAll(List.of(
                new Review(ana, zelda, 5, "obra prima"),
                new Review(ana, hades, 4, "viciante")
        ));
        collectionRepository.save(new CollectionEntry(ana, zelda, 120, CollectionStatus.ZERADO));

        GameActivityService service =
                new GameActivityService(userRepository, reviewRepository, collectionRepository);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new GameActivityController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("expoe genero e nota de cada jogo avaliado")
    void exposesGenreAndRatingOfEachRatedGame() throws Exception {
        mockMvc.perform(get("/api/users/ana/game-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.ratedGames.length()").value(2))
                // O genero e o motivo deste endpoint existir: sem ele o
                // microsservico nao consegue calcular afinidade.
                .andExpect(jsonPath("$.ratedGames[?(@.rating == 5)].genre").value("Aventura"))
                .andExpect(jsonPath("$.ratedGames[?(@.rating == 4)].genre").value("Roguelike, Indie"));
    }

    @Test
    @DisplayName("expoe os ids dos jogos que o usuario ja tem")
    void exposesOwnedGameIds() throws Exception {
        mockMvc.perform(get("/api/users/ana/game-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownedGameIds.length()").value(1));
    }

    @Test
    @DisplayName("usuario sem atividade devolve listas vazias, nao erro")
    void userWithoutActivityGetsEmptyLists() throws Exception {
        // Importa que seja 200 com listas vazias: o microsservico trata "sem
        // atividade" recomendando os melhores da comunidade. Se aqui viesse erro,
        // usuario novo ficaria sem recomendacao nenhuma.
        mockMvc.perform(get("/api/users/novato/game-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratedGames.length()").value(0))
                .andExpect(jsonPath("$.ownedGameIds.length()").value(0));
    }

    @Test
    @DisplayName("usuario inexistente devolve 404")
    void unknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/users/ninguem/game-activity"))
                .andExpect(status().isNotFound());
    }
}
