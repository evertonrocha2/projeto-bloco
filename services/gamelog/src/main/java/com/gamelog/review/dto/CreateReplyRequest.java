package com.gamelog.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O que alguem manda pra responder uma avaliacao.
//
// parentId nulo significa "pendurada direto na avaliacao". Nao e um campo
// obrigatorio com valor especial: e a ausencia de pai, que e o caso mais comum.
public record CreateReplyRequest(

        @NotBlank(message = "escreva alguma coisa")
        @Size(max = 1000, message = "a resposta passou de 1000 caracteres")
        String text,

        Long parentId
) {
}
