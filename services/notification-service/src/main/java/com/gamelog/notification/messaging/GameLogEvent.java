package com.gamelog.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

// O envelope dos eventos do GameLog, do jeito que ESTE servico le.
//
// Copia independente da do recommendation-service, de proposito: consumidores
// compartilham o contrato JSON, nao uma biblioteca. Assim cada um evolui (e le so
// os campos que usa) sem precisar republicar todo mundo junto.
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameLogEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String source,
        String aggregateId,
        JsonNode payload
) {
}
