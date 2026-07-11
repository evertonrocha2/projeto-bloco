package com.gamelog.review.domain;

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

// Review e o coracao do app: liga um usuario a um jogo, com uma nota e um texto.
// A constraint de unicidade (user_id, game_id) garante que cada pessoa so
// escreve uma review por jogo - isso e uma regra de negocio do dominio.
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"})
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: muitas reviews apontam pro mesmo usuario. LAZY pra so carregar
    // o usuario do banco quando a gente realmente acessar ele.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    // Nota de 0 a 5. A validacao do intervalo acontece no service.
    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Review() {
    }

    public Review(User user, Game game, int rating, String text) {
        this.user = user;
        this.game = game;
        this.rating = rating;
        this.text = text;
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

    public int getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
