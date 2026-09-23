package com.gamelog.notification.service;

import com.gamelog.notification.domain.Notification;
import com.gamelog.notification.dto.FeedItemResponse;
import com.gamelog.notification.dto.NotificationResponse;
import com.gamelog.notification.dto.NotificationsResponse;
import com.gamelog.notification.repository.FeedItemRepository;
import com.gamelog.notification.repository.NotificationRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Leitura e marcacao das notificacoes - o lado sincrono (HTTP) do servico.
@Service
public class NotificationService {

    private static final int MAX_PAGE = 50;

    private final NotificationRepository notificationRepository;
    private final FeedItemRepository feedItemRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               FeedItemRepository feedItemRepository,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.feedItemRepository = feedItemRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse inbox(String username, int limit) {
        List<NotificationResponse> items = notificationRepository
                .findByRecipientOrderByCreatedAtDescIdDesc(username, PageRequest.of(0, clamp(limit)))
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationsResponse(username,
                notificationRepository.countByRecipientAndReadAtIsNull(username), items);
    }

    @Transactional
    public NotificationResponse markRead(String username, Long id) {
        Notification notification = notificationRepository.findByIdAndRecipient(id, username)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.markRead(clock.instant());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(String username) {
        return notificationRepository.markAllRead(username, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<FeedItemResponse> feed(int limit) {
        return feedItemRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, clamp(limit)))
                .stream()
                .map(FeedItemResponse::from)
                .toList();
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, MAX_PAGE));
    }
}
