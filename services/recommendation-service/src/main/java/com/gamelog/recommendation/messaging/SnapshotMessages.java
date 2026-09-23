package com.gamelog.recommendation.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

// As mensagens do request/reply de snapshot, do lado de quem pede.
public final class SnapshotMessages {

    private SnapshotMessages() {
    }

    public record SnapshotRequest(String requestedBy) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogSnapshot(
            Instant generatedAt,
            List<EventPayloads.GameEvent> games,
            List<Rating> ratings,
            List<Ownership> collection
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rating(String username, Long gameId, int rating) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ownership(String username, Long gameId, String status) {
    }
}
