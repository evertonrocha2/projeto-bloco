package com.gamelog.recommendation.projection;

import java.util.Set;

// O que aconteceu ao aplicar um evento na projecao, pra quem chamou decidir o
// proximo passo (disparar recalculo de quem).
//
//  duplicate      - o evento ja tinha sido aplicado; nada mudou.
//  affectedUsers  - usuarios cuja atividade mudou (nota ou colecao).
//  catalogChanged - entrou jogo novo: muda os candidatos de TODO mundo.
public record ProjectionOutcome(boolean duplicate, Set<String> affectedUsers, boolean catalogChanged) {

    public static ProjectionOutcome duplicateEvent() {
        return new ProjectionOutcome(true, Set.of(), false);
    }

    public static ProjectionOutcome forUser(String username) {
        return new ProjectionOutcome(false, Set.of(username), false);
    }

    public static ProjectionOutcome catalog() {
        return new ProjectionOutcome(false, Set.of(), true);
    }

    public static ProjectionOutcome nothing() {
        return new ProjectionOutcome(false, Set.of(), false);
    }
}
