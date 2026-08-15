package com.gamelog.recommendation.client.dto;

import java.util.List;

// Resposta de GET /api/users/{username}/game-activity no monolito.
public record GameActivityPayload(
        String username,
        List<RatedGamePayload> ratedGames,
        List<Long> ownedGameIds
) {
}
