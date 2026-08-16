package com.gamelog.catalog.dto;

import com.gamelog.catalog.domain.Game;
import com.gamelog.review.dto.RatingStats;

// Como um jogo aparece na listagem do catalogo. Alem dos dados do jogo,
// junta a media de notas e o numero de reviews pra mostrar no card.
public record GameResponse(
        Long id,
        String title,
        String description,
        Integer releaseYear,
        String genre,
        String coverUrl,
        double averageRating,
        int reviewCount
) {
    public static GameResponse from(Game game, RatingStats stats) {
        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getReleaseYear(),
                game.getGenre(),
                game.getCoverUrl(),
                stats.average(),
                stats.count()
        );
    }
}
