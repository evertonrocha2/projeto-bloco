package com.gamelog.notification.repository;

import com.gamelog.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDescIdDesc(String recipient, Pageable page);

    long countByRecipientAndReadAtIsNull(String recipient);

    Optional<Notification> findByIdAndRecipient(Long id, String recipient);

    @Modifying
    @Query("update Notification n set n.readAt = :when where n.recipient = :recipient and n.readAt is null")
    int markAllRead(@Param("recipient") String recipient, @Param("when") Instant when);
}
