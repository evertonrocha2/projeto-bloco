package com.gamelog.notification.dto;

import java.util.List;

public record NotificationsResponse(String username, long unread, List<NotificationResponse> items) {
}
