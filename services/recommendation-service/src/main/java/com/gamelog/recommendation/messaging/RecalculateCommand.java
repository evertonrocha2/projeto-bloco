package com.gamelog.recommendation.messaging;

import java.time.Instant;

// Comando "recalcule as recomendacoes de username".
//
// === Evento x comando ===
//
// Evento (review.created) e um FATO no passado, anunciado pra quem quiser ouvir;
// quem publica nao espera nada de ninguem. Comando e um PEDIDO com um destinatario
// so, que deve ser executado uma vez. Por isso vivem em exchanges diferentes: o
// evento vai pro topic compartilhado, o comando pro exchange direct deste servico.
//
// causationId e o eventId que provocou o comando. Nos logs e no rastreamento da pra
// seguir a cadeia: review.created (evt-123) -> recalcular "ana" (causado por evt-123).
public record RecalculateCommand(String username, String reason, String causationId, Instant requestedAt) {
}
