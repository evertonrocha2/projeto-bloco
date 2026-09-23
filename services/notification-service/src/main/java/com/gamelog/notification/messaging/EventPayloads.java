package com.gamelog.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Os campos dos payloads que interessam as notificacoes. Tudo que o monolito
// manda a mais e ignorado (tolerant reader).
public final class EventPayloads {

    private EventPayloads() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewPublished(Long reviewId, String username, Long gameId, String gameTitle, Integer rating) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewReplied(Long reviewId, Long replyId, String reviewAuthor, String replyAuthor,
                                String parentAuthor, Long gameId, String gameTitle, String excerpt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewVoted(Long reviewId, String reviewAuthor, String voter, String voteType,
                              Long gameId, String gameTitle) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRegistered(String username) {
    }
}
