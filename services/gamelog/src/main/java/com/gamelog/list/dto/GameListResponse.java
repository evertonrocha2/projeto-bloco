package com.gamelog.list.dto;

import com.gamelog.list.domain.GameList;
import java.time.Instant;
import java.util.List;
import java.util.Set;

// Uma lista aberta: cabecalho, tags e todos os jogos com suas notas.
//
// visibility vai como String, e nao como enum, pelo mesmo motivo do status da
// colecao: a tela compara codigo, e o rotulo e problema dela.
public record GameListResponse(
        Long id,
        String owner,
        String title,
        String description,
        String coverUrl,
        String visibility,
        Set<String> tags,
        Instant createdAt,
        List<GameListItemResponse> items
) {
    public static GameListResponse from(GameList list) {
        return new GameListResponse(
                list.getId(),
                list.getOwner().getUsername(),
                list.getTitle(),
                list.getDescription(),
                list.getCoverUrl(),
                list.getVisibility().name(),
                list.getTags(),
                list.getCreatedAt(),
                list.getItems().stream().map(GameListItemResponse::from).toList()
        );
    }
}
