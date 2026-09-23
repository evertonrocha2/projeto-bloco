package com.gamelog.messaging.event;

import com.gamelog.collection.domain.CollectionEntry;

// Payload de collection.updated - um jogo entrou na colecao ou mudou de status.
//
// previousStatus e nulo quando o jogo acabou de entrar. Com ele o consumidor
// distingue "zerou agora" de "ja estava zerado e so corrigiu as horas" sem guardar
// o estado anterior por conta propria.
public record CollectionEventPayload(
        Long entryId,
        String username,
        Long gameId,
        String gameTitle,
        String genre,
        String status,
        String previousStatus,
        int hoursPlayed
) {
    public static CollectionEventPayload from(CollectionEntry entry, String previousStatus) {
        return new CollectionEventPayload(
                entry.getId(),
                entry.getUser().getUsername(),
                entry.getGame().getId(),
                entry.getGame().getTitle(),
                entry.getGame().getGenre(),
                entry.getStatus().name(),
                previousStatus,
                entry.getHoursPlayed());
    }
}
