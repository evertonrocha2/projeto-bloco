package com.gamelog.recommendation.dto;

import com.gamelog.recommendation.domain.Recommendation;
import java.util.List;

// Uma recomendacao como a API a devolve.
public record RecommendationItem(
        Long gameId,
        String gameTitle,
        String gameCoverUrl,
        double score,
        // Generos que justificam a indicacao. Lista vazia = veio da nota da
        // comunidade, e a tela escreve o texto correspondente.
        List<String> reasonGenres
) {
    public static RecommendationItem from(Recommendation recommendation) {
        return new RecommendationItem(
                recommendation.getGameId(),
                recommendation.getGameTitle(),
                recommendation.getGameCoverUrl(),
                recommendation.getScore(),
                recommendation.getReasonGenreList());
    }
}
