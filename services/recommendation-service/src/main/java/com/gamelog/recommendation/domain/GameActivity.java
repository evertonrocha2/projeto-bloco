package com.gamelog.recommendation.domain;

import java.util.List;

// O que o usuario ja fez no GameLog: o que avaliou e o que tem na colecao.
//
// E o retrato que o microsservico busca no monolito pra poder recomendar. Note
// que ele nao guarda esse retrato como dado seu: e insumo de calculo, buscado na
// hora. O que o microsservico persiste sao as recomendacoes geradas e o feedback
// - dados que so existem aqui.
public record GameActivity(
        String username,
        List<RatedGame> ratedGames,
        // So os ids: servem pra excluir candidatos. O genero desses jogos, quando
        // necessario, e resolvido pelo catalogo.
        List<Long> ownedGameIds
) {
    public static GameActivity empty(String username) {
        return new GameActivity(username, List.of(), List.of());
    }
}
