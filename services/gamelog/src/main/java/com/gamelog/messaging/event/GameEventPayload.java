package com.gamelog.messaging.event;

import com.gamelog.catalog.domain.Game;

// Payload de catalog.game.added, e tambem a forma de um jogo dentro do snapshot.
public record GameEventPayload(
        Long gameId,
        String title,
        String genre,
        String coverUrl,
        Integer releaseYear
) {
    public static GameEventPayload from(Game game) {
        return new GameEventPayload(
                game.getId(), game.getTitle(), game.getGenre(), game.getCoverUrl(), game.getReleaseYear());
    }
}
