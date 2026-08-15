package com.gamelog.recommendation.domain;

// Os parametros que regulam o algoritmo de recomendacao.
//
// Sao um VALOR passado pro algoritmo, nao uma dependencia dele. Por isso este
// record e puro: nenhuma anotacao do Spring, nenhuma leitura de arquivo. Quem
// traduz configuracao em pesos e o ScoringProperties, na camada de config -
// assim o algoritmo pode ser testado com qualquer combinacao de numeros, sem
// subir contexto nenhum.
public record ScoringWeights(
        // Nota minima pra considerar que a pessoa gostou do jogo.
        int minRating,
        // Peso de um jogo que esta na colecao mas nao foi avaliado.
        double collectionWeight,
        // Reforco pro genero de um jogo marcado como "gostei" nas recomendacoes.
        double likedBoost,
        // Quanto a afinidade de genero vale no score final.
        double genreWeight,
        // Quanto a nota media da comunidade vale no score final.
        double communityWeight,
        // Quantas recomendacoes devolver.
        int maxResults
) {
}
