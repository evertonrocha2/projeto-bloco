package com.gamelog.collection.dto;

import jakarta.validation.constraints.Min;
import com.gamelog.collection.domain.CollectionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotNull;

// O que o usuario manda pra colocar (ou atualizar) um jogo na colecao.
public record AddToCollectionRequest(
        @NotNull(message = "informe o jogo")
        Long gameId,

        @Min(value = 0, message = "horas nao pode ser negativo")
        int hoursPlayed,

        // Enum, e nao String: o Spring recusa um valor fora da lista antes de
        // chegar ao service. Com String livre, "Zeradoo" viraria uma lista
        // fantasma que nenhuma tela sabe mostrar.
        @NotNull(message = "informe o status")
        CollectionStatus status
) {
}
