package com.gamelog.list.dto;

import com.gamelog.list.domain.GameListItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Um jogo entrando numa lista, com o comentario de quem o colocou ali.
//
// A nota e opcional: nem todo jogo precisa de justificativa, e exigir uma faria
// montar uma lista de vinte jogos virar redacao.
public record AddListItemRequest(

        @NotNull(message = "informe o jogo")
        Long gameId,

        @Size(max = GameListItem.MAX_NOTE_LENGTH, message = "a nota passou de 280 caracteres")
        String note
) {
}
