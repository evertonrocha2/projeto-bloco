package com.gamelog.recommendation.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

// Um jogo na colecao de um usuario, copiado dos eventos collection.updated.
// Mesma regra de lastEventAt da UserRatingView.
@Entity
@Table(
        name = "user_collection",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "game_id"}),
        indexes = @Index(name = "idx_user_collection_username", columnList = "username")
)
public class UserCollectionView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    protected UserCollectionView() {
    }

    public UserCollectionView(String username, Long gameId, String status, Instant lastEventAt) {
        this.username = username;
        this.gameId = gameId;
        this.status = status;
        this.lastEventAt = lastEventAt;
    }

    public boolean isNewerThan(Instant when) {
        return lastEventAt.isAfter(when);
    }

    public void update(String status, Instant when) {
        this.status = status;
        this.lastEventAt = when;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }
}
