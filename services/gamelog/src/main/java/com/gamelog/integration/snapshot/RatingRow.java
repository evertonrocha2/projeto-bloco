package com.gamelog.integration.snapshot;

// Uma nota dentro do snapshot. Alvo de projecao JPQL (select new ...).
public record RatingRow(String username, Long gameId, int rating) {
}
