package com.gamelog.recommendation.domain;

// Um veredito do usuario sobre um jogo recomendado, na forma que o algoritmo
// consome. A entidade persistida (RecommendationFeedback) e outra coisa: esta
// aqui e so valor, sem id nem data, pra manter o algoritmo puro.
public record FeedbackEntry(Long gameId, FeedbackVerdict verdict) {
}
