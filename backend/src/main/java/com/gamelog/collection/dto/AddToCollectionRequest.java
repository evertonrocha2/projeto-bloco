package com.gamelog.collection.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// O que o usuario manda pra colocar (ou atualizar) um jogo na colecao.
public record AddToCollectionRequest(
        @NotNull(message = "informe o jogo")
        Long gameId,

        @Min(value = 0, message = "horas nao pode ser negativo")
        int hoursPlayed,

        @NotBlank(message = "informe o status")
        String status
) {
}
