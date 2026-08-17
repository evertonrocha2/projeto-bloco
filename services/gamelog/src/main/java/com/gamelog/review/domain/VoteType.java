package com.gamelog.review.domain;

// De que lado uma pessoa ficou sobre uma avaliacao alheia.
//
// Sao dois contadores separados na tela (12 positivos, 3 negativos), e nao um
// saldo unico. Saldo esconde o volume: "+9" tanto pode ser nove pessoas de
// acordo quanto trinta contra vinte e uma, que sao situacoes bem diferentes pra
// quem le a avaliacao pra decidir se compra o jogo.
public enum VoteType {

    POSITIVE,

    NEGATIVE
}
