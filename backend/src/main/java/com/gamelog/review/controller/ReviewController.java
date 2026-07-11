package com.gamelog.review.controller;

import com.gamelog.review.dto.CreateReviewRequest;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.service.ReviewService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Criar review. A rota fica "embaixo" de um jogo (/api/games/{id}/reviews)
// porque uma review sempre pertence a um jogo. Essa rota exige login: o
// SecurityConfig so deixa passar quem tem token valido, e o Principal nos diz
// quem e o autor sem precisar mandar o username no corpo (mais seguro).
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
}
