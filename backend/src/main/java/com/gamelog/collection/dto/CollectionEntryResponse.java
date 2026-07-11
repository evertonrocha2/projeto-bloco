package com.gamelog.collection.dto;

import com.gamelog.collection.domain.CollectionEntry;
import java.time.Instant;

// Como um item da colecao aparece pra fora. Inclui a capa e o titulo do jogo
// pra tela conseguir montar o card sem precisar buscar o jogo separado.
public record CollectionEntryResponse(
        Long id,
        Long gameId,
        String gameTitle,
        String gameCoverUrl,
        int hoursPlayed,
        String status,
        Instant createdAt
) {
    public static CollectionEntryResponse from(CollectionEntry entry) {
        return new CollectionEntryResponse(
                entry.getId(),
                entry.getGame().getId(),
                entry.getGame().getTitle(),
                entry.getGame().getCoverUrl(),
                entry.getHoursPlayed(),
                entry.getStatus(),
                entry.getCreatedAt()
        );
    }
}
