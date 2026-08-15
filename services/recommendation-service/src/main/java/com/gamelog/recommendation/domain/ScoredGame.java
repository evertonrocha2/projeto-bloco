package com.gamelog.recommendation.domain;

import java.util.List;

// Um jogo recomendado, com a nota que o algoritmo deu e a justificativa.
//
// reasonGenres carrega os GENEROS que explicam a recomendacao, nao uma frase
// pronta. A frase ("porque voce gosta de RPG e Action") e montada no front. Duas
// razoes: apresentacao - com acentuacao e idioma - e assunto da camada de
// interface, e dado estruturado permite a tela fazer mais coisas com ele (grifar
// o genero, filtrar, virar link) do que uma string fechada permitiria.
//
// Lista vazia significa "nao ha afinidade a citar": e o caso do usuario novo, e a
// tela mostra que a indicacao veio da nota da comunidade.
public record ScoredGame(
        Long gameId,
        String title,
        String coverUrl,
        double score,
        List<String> reasonGenres
) {
}
