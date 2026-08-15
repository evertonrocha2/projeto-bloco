package com.gamelog.recommendation.domain;

// O que o usuario achou de uma recomendacao.
//
// Este e o vocabulario que o microsservico inventa: o monolito nao tem conceito
// de "gostei da recomendacao". E o dado exclusivo deste contexto.
public enum FeedbackVerdict {
    // Curtiu: reforca o genero do jogo no perfil de gosto.
    LIKED,
    // Nao interessa: o jogo sai das recomendacoes e nao volta.
    DISMISSED
}
