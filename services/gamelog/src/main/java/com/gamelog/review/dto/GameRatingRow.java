package com.gamelog.review.dto;

// Linha de resultado da consulta agregada de notas (uma por jogo).
// Usa wrappers (Double/Long) porque e isso que o AVG e o COUNT do JPQL devolvem.
public record GameRatingRow(Long gameId, Double average, Long count) {

    public RatingStats toStats() {
        return new RatingStats(
                average == null ? 0.0 : average,
                count == null ? 0 : count.intValue()
        );
    }
}
