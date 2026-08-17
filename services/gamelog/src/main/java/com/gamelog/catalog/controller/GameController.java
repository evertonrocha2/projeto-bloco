package com.gamelog.catalog.controller;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.dto.GameDetailResponse;
import com.gamelog.catalog.dto.GamePageResponse;
import com.gamelog.catalog.dto.GameResponse;
import com.gamelog.catalog.service.GameService;
import com.gamelog.review.dto.RatingStats;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.dto.ReviewSocial;
import com.gamelog.review.service.ReviewService;
import com.gamelog.review.service.ReviewSocialService;
import java.security.Principal;
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
    private final ReviewSocialService reviewSocialService;

    public GameController(GameService gameService,
                          ReviewService reviewService,
                          ReviewSocialService reviewSocialService) {
        this.gameService = gameService;
        this.reviewService = reviewService;
        this.reviewSocialService = reviewSocialService;
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

    // Pagina de um jogo: dados + resumo das notas + todas as reviews, cada uma
    // com seus votos e sua arvore de respostas.
    //
    // O social e carregado EM LOTE, uma vez pra pagina inteira. Buscar por review
    // seriam tres consultas por avaliacao listada, e o custo cresceria com a
    // popularidade do jogo - a pagina ficaria mais lenta justamente onde mais
    // gente entra.
    //
    // O Principal e anulavel: esta rota e publica, entao quem nao esta logado ve
    // as contagens sem ter voto proprio marcado.
    @GetMapping("/{id}")
    public GameDetailResponse getById(@PathVariable Long id, Principal principal) {
        Game game = gameService.findById(id);
        List<ReviewResponse> reviews = reviewService.findByGame(id);

        String viewer = principal == null ? null : principal.getName();
        Map<Long, ReviewSocial> social = reviewSocialService.loadFor(
                reviews.stream().map(ReviewResponse::id).toList(), viewer);

        List<ReviewResponse> comSocial = reviews.stream()
                .map(review -> review.withSocial(
                        social.getOrDefault(review.id(), ReviewSocial.empty())))
                .toList();

        return GameDetailResponse.from(game, reviewService.statsForGame(id), comSocial);
    }
}
