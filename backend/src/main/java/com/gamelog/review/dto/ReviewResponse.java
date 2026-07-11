package com.gamelog.review.dto;

import com.gamelog.review.domain.Review;

import java.time.Instant;

// Como uma review aparece pra fora. Repare que aqui a gente "achata" a entidade:
// em vez de mandar o objeto User/Game inteiro, manda so o que a tela precisa
// (username, titulo e capa do jogo). Assim nao vaza dado interno nem senha.
public record ReviewResponse(
        Long id,
        int rating,
        String text,
        Instant createdAt,
        String username,
        Long gameId,
        String gameTitle,
        String gameCoverUrl
) {
    // Fabrica que converte a entidade do banco no DTO da API.
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getText(),
                review.getCreatedAt(),
                review.getUser().getUsername(),
                review.getGame().getId(),
                review.getGame().getTitle(),
                review.getGame().getCoverUrl()
        );
    }
}
