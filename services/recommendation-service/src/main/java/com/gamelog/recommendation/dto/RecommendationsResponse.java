package com.gamelog.recommendation.dto;

import java.time.Instant;
import java.util.List;

// A resposta da tela de recomendacoes.
public record RecommendationsResponse(
        String username,
        // Quando o lote foi calculado. Null se nunca houve lote.
        Instant generatedAt,
        // true = esta resposta NAO refletiu uma conversa bem-sucedida com o
        // monolito; o que esta aqui e o ultimo lote gravado (ou nada).
        //
        // Expor isso na API em vez de esconder e uma decisao de honestidade: o
        // front acende um aviso de "modo degradado" e o usuario entende por que a
        // lista talvez nao tenha mudado. Fingir normalidade seria pior - ele
        // clicaria em "recalcular" varias vezes sem entender o que acontece.
        boolean stale,
        List<RecommendationItem> items
) {
}
