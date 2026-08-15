package com.gamelog.recommendation.dto;

import com.gamelog.recommendation.domain.FeedbackVerdict;
import jakarta.validation.constraints.NotNull;

// Corpo de POST /api/recommendations/{username}/feedback.
//
// O verdict e recebido como enum: se vier um valor que nao existe, o Spring
// recusa a requisicao antes de chegar ao service. Aceitar String e converter na
// mao daria a chance de um valor invalido virar um estado sem significado no
// banco.
public record FeedbackRequest(
        @NotNull(message = "gameId e obrigatorio")
        Long gameId,

        @NotNull(message = "verdict e obrigatorio (LIKED ou DISMISSED)")
        FeedbackVerdict verdict
) {
}
