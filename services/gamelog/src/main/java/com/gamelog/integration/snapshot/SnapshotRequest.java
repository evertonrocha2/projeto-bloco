package com.gamelog.integration.snapshot;

// O pedido de snapshot. requestedBy serve pro log: saber qual servico pediu.
public record SnapshotRequest(String requestedBy) {
}
