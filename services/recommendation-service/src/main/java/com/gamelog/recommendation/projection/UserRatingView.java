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

// A nota que um usuario deu a um jogo, copiada dos eventos review.*.
//
// === Por que lastEventAt ===
//
// A projecao so aplica um evento se ele for MAIS NOVO que o que ja esta aqui. E a
// defesa contra evento fora de ordem: um review.updated atrasado (reentregue
// depois de uma falha, por exemplo) nao pode sobrescrever uma nota mais recente.
//
// === Por que "deleted" em vez de apagar a linha ===
//
// Se review.deleted apagasse a linha, a projecao esqueceria QUANDO a review foi
// apagada - e um review.updated mais antigo chegando depois recriaria a nota. A
// linha fica como lapide (tombstone), com a data do delete, e continua barrando
// eventos mais velhos que ela. As consultas simplesmente ignoram as lapides.
@Entity
@Table(
        name = "user_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "game_id"}),
        indexes = @Index(name = "idx_user_ratings_username", columnList = "username")
)
public class UserRatingView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    protected UserRatingView() {
    }

    public UserRatingView(String username, Long gameId, int rating, Instant lastEventAt) {
        this.username = username;
        this.gameId = gameId;
        this.rating = rating;
        this.lastEventAt = lastEventAt;
    }

    public boolean isNewerThan(Instant when) {
        return lastEventAt.isAfter(when);
    }

    public void rate(int rating, Instant when) {
        this.rating = rating;
        this.deleted = false;
        this.lastEventAt = when;
    }

    public void markDeleted(Instant when) {
        this.deleted = true;
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

    public int getRating() {
        return rating;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }
}
