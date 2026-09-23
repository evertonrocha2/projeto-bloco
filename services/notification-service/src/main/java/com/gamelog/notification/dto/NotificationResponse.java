package com.gamelog.notification.dto;

import com.gamelog.notification.domain.Notification;
import com.gamelog.notification.domain.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        String actor,
        Long reviewId,
        Long gameId,
        Instant createdAt,
        boolean read
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getActor(),
                n.getReviewId(), n.getGameId(), n.getCreatedAt(), n.isRead());
    }
}
