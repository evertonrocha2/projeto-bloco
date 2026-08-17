package com.gamelog.review.dto;

import com.gamelog.review.domain.VoteType;
import jakarta.validation.constraints.NotNull;

// De que lado a pessoa esta votando.
//
// Enum, e nao String: {"type":"banana"} e recusado com 400 pelo
// GlobalExceptionHandler antes de chegar ao service - mesmo caminho que o
// CollectionStatus tomou depois de aceitar texto livre por tempo demais.
public record VoteRequest(

        @NotNull(message = "informe o tipo do voto")
        VoteType type
) {
}
