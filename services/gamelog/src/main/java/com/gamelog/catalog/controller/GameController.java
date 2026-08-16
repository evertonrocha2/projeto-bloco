package com.gamelog.catalog.controller;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.dto.GameDetailResponse;
import com.gamelog.catalog.dto.GamePageResponse;
import com.gamelog.catalog.dto.GameResponse;
import com.gamelog.catalog.service.GameService;
import com.gamelog.review.dto.RatingStats;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.service.ReviewService;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    // As medias vem TODAS numa unica consulta agregada (statsForGames), em vez
    // de uma consulta por jogo - ver o comentario no ReviewRepository.
    @GetMapping
    public List<GameResponse> list() {
        List<Game> games = gameService.findAll();
        Map<Long, RatingStats> stats = reviewService.statsForGames(
                games.stream().map(Game::getId).toList());

        return games.stream()
                .map(game -> GameResponse.from(game,
                        stats.getOrDefault(game.getId(), RatingStats.empty())))
                .toList();
    }

    // Busca paginada por titulo: /api/games/search?title=zelda&page=0&size=12
    // O banco so devolve a pagina pedida - importante quando o catalogo cresce.
    @GetMapping("/search")
    public GamePageResponse search(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Page<Game> result = gameService.search(title,
                PageRequest.of(page, Math.min(size, 50), Sort.by("title").ascending()));

        Map<Long, RatingStats> stats = reviewService.statsForGames(
                result.getContent().stream().map(Game::getId).toList());

        List<GameResponse> content = result.getContent().stream()
                .map(game -> GameResponse.from(game,
                        stats.getOrDefault(game.getId(), RatingStats.empty())))
                .toList();

        return new GamePageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // Pagina de um jogo: dados + resumo das notas + todas as reviews.
    @GetMapping("/{id}")
    public GameDetailResponse getById(@PathVariable Long id) {
        Game game = gameService.findById(id);
        List<ReviewResponse> reviews = reviewService.findByGame(id);
        return GameDetailResponse.from(game, reviewService.statsForGame(id), reviews);
    }
}
