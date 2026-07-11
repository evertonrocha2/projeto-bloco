package com.gamelog.catalog.dto;

import com.gamelog.catalog.domain.Game;
import com.gamelog.review.dto.RatingStats;
import com.gamelog.review.dto.ReviewResponse;

import java.util.List;

// Pagina de um jogo especifico: os dados do jogo, o resumo das notas e a
// lista completa de reviews que ele recebeu.
public record GameDetailResponse(
        Long id,
        String title,
        String description,
        Integer releaseYear,
        String genre,
        String coverUrl,
        double averageRating,
        int reviewCount,
        List<ReviewResponse> reviews
) {
    public static GameDetailResponse from(Game game, RatingStats stats, List<ReviewResponse> reviews) {
        return new GameDetailResponse(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getReleaseYear(),
                game.getGenre(),
                game.getCoverUrl(),
                stats.average(),
                stats.count(),
                reviews
        );
    }
}
