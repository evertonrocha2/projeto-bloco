package com.gamelog.review.controller;

import com.gamelog.review.dto.CreateReviewRequest;
import com.gamelog.review.dto.ReviewRevisionResponse;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.service.ReviewService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Rotas de review. Criar fica "embaixo" de um jogo (/api/games/{id}/reviews)
// porque uma review sempre pertence a um jogo; editar, apagar e ver historico
// usam o id da propria review. Todas exigem login: o SecurityConfig so deixa
// passar quem tem token valido, e o Principal nos diz quem e o autor.
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/games/{gameId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @PathVariable Long gameId,
            @Valid @RequestBody CreateReviewRequest request,
            Principal principal
    ) {
        // principal.getName() e o username de quem mandou o token.
        return reviewService.create(principal.getName(), gameId, request);
    }

    // Editar a propria review (nota e/ou texto). Cada edicao vira uma revisao
    // no historico.
    @PutMapping("/api/reviews/{reviewId}")
    public ReviewResponse update(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewRequest request,
            Principal principal
    ) {
        return reviewService.update(principal.getName(), reviewId, request);
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long reviewId, Principal principal) {
        reviewService.delete(principal.getName(), reviewId);
    }

    // Historico de mudancas da review: cada item e uma revisao (INSERT/UPDATE/
    // DELETE) com o estado da epoca, quem mudou e quando.
    @GetMapping("/api/reviews/{reviewId}/history")
    public List<ReviewRevisionResponse> history(@PathVariable Long reviewId, Principal principal) {
        return reviewService.history(principal.getName(), reviewId);
    }
}
