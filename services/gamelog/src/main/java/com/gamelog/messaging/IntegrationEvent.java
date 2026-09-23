package com.gamelog.messaging;

import java.time.Instant;

// O envelope de todo evento que sai do monolito.
//
// Os campos do envelope sao os mesmos pra qualquer evento; so o payload muda. Isso
// permite a um consumidor tratar o que e comum (deduplicar, ordenar, rastrear) sem
// saber nada do assunto do evento:
//
//  eventId       - identidade da MENSAGEM. A entrega e "pelo menos uma vez", entao
//                  o mesmo evento pode chegar duas vezes; o consumidor usa este id
//                  pra reconhecer a repeticao e ignorar (consumidor idempotente).
//  eventType     - o que aconteceu, e a routing key no exchange.
//  schemaVersion - versao do formato do payload. Mudanca incompativel sobe o numero
//                  e o consumidor decide o que fazer com a versao que nao conhece,
//                  em vez de quebrar desserializando um formato novo.
//  occurredAt    - quando o fato aconteceu NO MONOLITO, nao quando chegou. E o que
//                  o consumidor usa pra descartar um evento mais velho que o estado
//                  que ele ja tem.
//  aggregateId   - de quem e o evento (id da review, username...). Util pra log e
//                  pra correlacionar eventos do mesmo agregado.
//
// O payload carrega o ESTADO relevante, e nao so "a review 7 mudou" (event-carried
// state transfer). Com so o id, todo consumidor teria que chamar o monolito de
// volta pra saber o que mudou - e o acoplamento sincrono que esta refatoracao
// existe pra tirar.
public record IntegrationEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String source,
        String aggregateId,
        Object payload
) {
}
