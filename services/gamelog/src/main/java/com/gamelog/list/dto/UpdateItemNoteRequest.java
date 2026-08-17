package com.gamelog.list.dto;

import com.gamelog.list.domain.GameListItem;
import jakarta.validation.constraints.Size;

// Editar o comentario de um jogo dentro de uma lista.
//
// Anulavel: apagar a nota e uma acao legitima - o jogo continua na lista, so sem
// justificativa escrita.
public record UpdateItemNoteRequest(

        @Size(max = GameListItem.MAX_NOTE_LENGTH, message = "a nota passou de 280 caracteres")
        String note
) {
}
