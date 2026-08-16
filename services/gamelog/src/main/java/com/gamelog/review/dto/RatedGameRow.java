package com.gamelog.review.dto;

// Uma avaliacao vista "de fora": qual jogo, de que genero, e que nota levou.
//
// Existe pro microsservico de recomendacoes. Ele precisa cruzar nota com genero
// pra montar o perfil de gosto do usuario, e o ReviewResponse (que o front usa)
// nao carrega genero - carrega titulo e capa, que e o que a tela precisa.
//
// Assim como o GameRatingRow, este record e o alvo de uma projecao JPQL: o
// Hibernate constroi a instancia direto no "select new ...", entao a consulta
// traz do banco somente estas tres colunas.
public record RatedGameRow(
        Long gameId,
        String genre,
        int rating
) {
}
