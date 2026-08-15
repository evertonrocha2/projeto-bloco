package com.gamelog.recommendation.domain;

// Um jogo do catalogo do monolito, visto como candidato a recomendacao.
//
// averageRating vem calculada pelo monolito (ele agrega no banco). O
// microsservico nao recalcula nem guarda nota: isso e dado do contexto de
// reviews, e duplicar a responsabilidade de calcular seria pedir pros dois
// numeros divergirem.
public record CatalogGame(
        Long gameId,
        String title,
        String coverUrl,
        String genre,
        double averageRating
) {
}
