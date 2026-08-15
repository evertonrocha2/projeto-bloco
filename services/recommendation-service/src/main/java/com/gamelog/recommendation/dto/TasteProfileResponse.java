package com.gamelog.recommendation.dto;

import java.util.List;

// O perfil de gosto do usuario, do genero mais forte pro mais fraco.
//
// Existe pra tornar a recomendacao AUDITAVEL. Sem ele, a lista de jogos e uma
// caixa preta: o usuario nao tem como saber se o sistema entendeu o gosto dele.
// Com o grafico na tela, "por que esse jogo apareceu?" tem resposta visivel - e,
// na apresentacao, e o que permite mostrar o algoritmo funcionando em vez de
// pedir pra confiar nele.
public record TasteProfileResponse(
        String username,
        List<GenreWeight> genres
) {
    // Peso de um genero, na escala em que o favorito vale 1.0.
    public record GenreWeight(String genre, double weight) {
    }
}
