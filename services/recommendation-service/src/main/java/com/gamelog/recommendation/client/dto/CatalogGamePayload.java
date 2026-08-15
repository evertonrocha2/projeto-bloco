package com.gamelog.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Um jogo do catalogo, como vem de GET /api/games no monolito.
//
// Atencao ao nome do primeiro campo: no catalogo o monolito chama o identificador
// de "id" (e "gameId" so na atividade). Trocar um pelo outro faria todo gameId
// chegar nulo, e nenhuma recomendacao seria gravada - sem erro de compilacao pra
// avisar. Ha um teste especifico travando isso.
//
// @JsonIgnoreProperties: o GameResponse do monolito tem mais campos do que os
// cinco daqui (descricao, ano, contagem de reviews). Ignorar o excedente e o que
// permite o monolito ACRESCENTAR campos sem quebrar este servico - tolerant
// reader, a regra basica de evolucao de contrato entre servicos.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogGamePayload(
        Long id,
        String title,
        String genre,
        String coverUrl,
        double averageRating
) {
}
