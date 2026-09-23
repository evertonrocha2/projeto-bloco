package com.gamelog.notification.dto;

import com.gamelog.notification.domain.FeedItem;
import java.time.Instant;

public record FeedItemResponse(Long reviewId, String username, Long gameId, String gameTitle,
                               Integer rating, Instant occurredAt) {

    public static FeedItemResponse from(FeedItem item) {
        return new FeedItemResponse(item.getReviewId(), item.getUsername(), item.getGameId(),
                item.getGameTitle(), item.getRating(), item.getOccurredAt());
    }
}
