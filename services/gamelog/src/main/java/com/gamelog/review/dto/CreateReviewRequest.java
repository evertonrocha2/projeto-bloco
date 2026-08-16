package com.gamelog.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O que o usuario manda pra avaliar um jogo. A nota tem que estar entre 0 e 5;
// isso e validado aqui (entrada) e tambem reforcado no service (regra de negocio).
public record CreateReviewRequest(
        @Min(value = 0, message = "nota minima e 0")
        @Max(value = 5, message = "nota maxima e 5")
        int rating,

        @NotBlank(message = "escreva um comentario")
        @Size(max = 2000, message = "comentario muito longo")
        String text
) {
}
