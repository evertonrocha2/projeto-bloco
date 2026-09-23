package com.gamelog.notification.repository;

import com.gamelog.notification.domain.FeedItem;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {

    List<FeedItem> findAllByOrderByOccurredAtDesc(Pageable page);
}
