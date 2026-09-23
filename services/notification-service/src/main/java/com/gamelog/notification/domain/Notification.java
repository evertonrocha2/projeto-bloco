package com.gamelog.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

// Um aviso na caixa de entrada de alguem.
//
// Guarda o texto JA montado e os dados que a tela precisa pra linkar (jogo,
// avaliacao). Nada aqui aponta pra tabela do monolito: username e gameId sao
// referencias por valor, como no recommendation-service.
@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_recipient", columnList = "recipient, created_at"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(length = 50)
    private String actor;

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "game_id")
    private Long gameId;

    // O evento que gerou o aviso. Serve pra rastrear "de onde veio isso" e aparece
    // nos logs junto com o traceId.
    @Column(name = "source_event_id", nullable = false, length = 36)
    private String sourceEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(String recipient, NotificationType type, String message, String actor,
                        Long reviewId, Long gameId, String sourceEventId, Instant createdAt) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.actor = actor;
        this.reviewId = reviewId;
        this.gameId = gameId;
        this.sourceEventId = sourceEventId;
        this.createdAt = createdAt;
    }

    public void markRead(Instant when) {
        if (readAt == null) {
            readAt = when;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getActor() {
        return actor;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
