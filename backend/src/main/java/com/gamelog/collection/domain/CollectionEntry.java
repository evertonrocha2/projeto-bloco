package com.gamelog.collection.domain;

import com.gamelog.catalog.domain.Game;
import com.gamelog.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

// Um item da colecao pessoal de um usuario: um jogo que ele marcou, com quantas
// horas jogou e em que pe esta (jogando, zerado, etc). E diferente de review -
// aqui nao precisa ter opiniao, e so o registro de que o jogo faz parte da vida
// dele. A constraint (user_id, game_id) garante um item por jogo por pessoa.
@Entity
@Table(
        name = "collection_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"})
)
public class CollectionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    // Horas jogadas (informadas pelo usuario).
    @Column(nullable = false)
    private int hoursPlayed;

    // Situacao do jogo na colecao: "Quero jogar", "Jogando", "Zerado", "Largado".
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected CollectionEntry() {
    }

    public CollectionEntry(User user, Game game, int hoursPlayed, String status) {
        this.user = user;
        this.game = game;
        this.hoursPlayed = hoursPlayed;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Game getGame() {
        return game;
    }

    public int getHoursPlayed() {
        return hoursPlayed;
    }

    // Da pra atualizar as horas e o status sem criar um item novo.
    public void setHoursPlayed(int hoursPlayed) {
        this.hoursPlayed = hoursPlayed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
