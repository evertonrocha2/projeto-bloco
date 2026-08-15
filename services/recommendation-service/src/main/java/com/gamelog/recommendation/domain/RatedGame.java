package com.gamelog.recommendation.domain;

// Um jogo que o usuario avaliou, do jeito que interessa pra ca: genero e nota.
// Vem do endpoint /api/users/{username}/game-activity do monolito.
public record RatedGame(Long gameId, String genre, int rating) {
}
