package com.gamelog.integration.dto;

import com.gamelog.review.dto.RatedGameRow;
import java.util.List;

// A "atividade de jogos" de um usuario: o que ele avaliou (com genero e nota) e
// o que ele tem na colecao.
//
// Este record e o CONTRATO entre o monolito e o microsservico de recomendacoes.
// Vale reparar no que ele NAO tem: nada de email, id interno, texto de review ou
// horas jogadas. O microsservico recebe exatamente o que precisa pra calcular
// afinidade de genero, e nada alem disso - mesmo raciocinio dos outros DTOs do
// projeto, aplicado agora a um consumidor que e outro servico em vez do front.
public record GameActivityResponse(
        String username,
        // Jogos avaliados. RatedGameRow ja e o formato que a projecao do
        // ReviewRepository devolve, entao nao existe conversao no meio.
        List<RatedGameRow> ratedGames,
        // Ids dos jogos na colecao. So os ids: aqui eles servem pra EXCLUIR
        // candidatos, e pra isso o id basta.
        List<Long> ownedGameIds
) {
}
