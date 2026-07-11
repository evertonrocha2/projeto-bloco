package com.gamelog.catalog.controller;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.dto.GameDetailResponse;
import com.gamelog.catalog.dto.GameResponse;
import com.gamelog.catalog.service.GameService;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Expoe o catalogo. Esse controller orquestra dois services: pega o jogo no
// GameService e as notas/reviews no ReviewService, e junta tudo num so JSON
// pro front. Cada service continua cuidando so do seu pedaco.
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final ReviewService reviewService;

    public GameController(GameService gameService, ReviewService reviewService) {
        this.gameService = gameService;
        this.reviewService = reviewService;
    }

    // Lista todos os jogos, cada um ja com sua media de nota e total de reviews.
    @GetMapping
    public List<GameResponse> list() {
        return gameService.findAll().stream()
                .map(game -> GameResponse.from(game, reviewService.statsForGame(game.getId())))
                .toList();
    }

    // Pagina de um jogo: dados + resumo das notas + todas as reviews.
    @GetMapping("/{id}")
    public GameDetailResponse getById(@PathVariable Long id) {
        Game game = gameService.findById(id);
        List<ReviewResponse> reviews = reviewService.findByGame(id);
        return GameDetailResponse.from(game, reviewService.statsForGame(id), reviews);
    }
}
