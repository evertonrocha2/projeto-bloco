package com.gamelog.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

// Uma linha do feed "o que a comunidade anda avaliando".
//
// Chave = id da avaliacao no monolito. Assim o review.deleted sabe exatamente
// qual linha tirar, e um review.created entregue duas vezes nao duplica o item.
@Entity
@Table(name = "feed_items",
        indexes = @Index(name = "idx_feed_items_occurred", columnList = "occurred_at"))
public class FeedItem {

    @Id
    @Column(name = "review_id")
    private Long reviewId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "game_title", nullable = false, length = 200)
    private String gameTitle;

    private Integer rating;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected FeedItem() {
    }

    public FeedItem(Long reviewId, String username, Long gameId, String gameTitle, Integer rating,
                    Instant occurredAt) {
        this.reviewId = reviewId;
        this.username = username;
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.rating = rating;
        this.occurredAt = occurredAt;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public String getUsername() {
        return username;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public Integer getRating() {
        return rating;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
