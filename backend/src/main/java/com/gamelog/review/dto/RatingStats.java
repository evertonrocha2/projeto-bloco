package com.gamelog.review.dto;

// Resumo das notas de um jogo: a media e quantas reviews ele tem.
// Calculado a partir das reviews e mostrado no card e na pagina do jogo.
public record RatingStats(double average, int count) {

    public static RatingStats empty() {
        return new RatingStats(0.0, 0);
    }
}
