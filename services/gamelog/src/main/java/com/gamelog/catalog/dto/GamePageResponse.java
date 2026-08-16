package com.gamelog.catalog.dto;

import java.util.List;

// Uma pagina do catalogo: os jogos daquela pagina + os numeros que o front
// precisa pra desenhar a paginacao. E a versao "achatada" do Page do Spring,
// pra API ter um contrato estavel (o JSON do Page interno pode mudar entre
// versoes do Spring Data).
public record GamePageResponse(
        List<GameResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
