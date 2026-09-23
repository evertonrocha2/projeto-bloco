package com.gamelog.integration.snapshot;

import com.gamelog.messaging.event.GameEventPayload;
import java.time.Instant;
import java.util.List;

// A resposta do pedido de snapshot: o estado inteiro que um consumidor precisa pra
// comecar a acompanhar os eventos.
//
// Por que existe, se os eventos ja carregam o estado: um consumidor que sobe pela
// PRIMEIRA vez (ou teve o banco apagado) nao viu os eventos do passado. O
// snapshot e o ponto de partida; dali em diante os eventos mantem a copia dele em
// dia. generatedAt diz a partir de que instante o snapshot vale - evento com
// occurredAt anterior ja esta refletido aqui.
//
// Pro volume deste projeto cabe numa mensagem so. Com milhoes de reviews o
// caminho seria paginar (varios pedidos com offset) ou reenviar os eventos
// historicos numa fila de replay - ver docs/EVENTOS.md.
public record CatalogSnapshot(
        Instant generatedAt,
        List<GameEventPayload> games,
        List<RatingRow> ratings,
        List<OwnershipRow> collection
) {
}
