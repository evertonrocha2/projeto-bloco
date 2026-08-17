package com.gamelog.list.dto;

import com.gamelog.list.domain.GameListItem;

// Um jogo da lista, como a tela o recebe.
//
// Traz titulo e capa junto do id: sem isso a tela precisaria buscar cada jogo
// separado pra desenhar a grade - um pedido por capa.
public record GameListItemResponse(
        Long id,
        Long gameId,
        String gameTitle,
        String gameCoverUrl,
        String note,
        int position
) {
    public static GameListItemResponse from(GameListItem item) {
        return new GameListItemResponse(
                item.getId(),
                item.getGame().getId(),
                item.getGame().getTitle(),
                item.getGame().getCoverUrl(),
                item.getNote(),
                item.getPosition()
        );
    }
}
