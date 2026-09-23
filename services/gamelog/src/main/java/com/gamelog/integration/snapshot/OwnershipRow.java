package com.gamelog.integration.snapshot;

// Um item de colecao dentro do snapshot.
public record OwnershipRow(String username, Long gameId, String status) {
}
