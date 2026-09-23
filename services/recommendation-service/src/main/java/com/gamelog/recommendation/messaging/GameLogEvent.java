package com.gamelog.recommendation.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

// Um evento do GameLog como este servico o recebe.
//
// O payload fica como JsonNode e so e convertido depois que o tipo do evento e
// conhecido. Isso permite um unico @RabbitListener pra fila inteira (que recebe
// review.*, collection.* e catalog.*) em vez de um metodo e uma fila por tipo.
//
// Este record e do CONSUMIDOR, e nao uma copia da classe do monolito: os dois lados
// compartilham o formato JSON, nao codigo. @JsonIgnoreProperties e o "tolerant
// reader" - o monolito pode acrescentar campos ao envelope sem quebrar ninguem.
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
