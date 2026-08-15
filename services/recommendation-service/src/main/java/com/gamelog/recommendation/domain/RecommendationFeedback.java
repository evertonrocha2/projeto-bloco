package com.gamelog.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

// O que o usuario achou de um jogo recomendado.
//
// Esta e a tabela que faz do banco deste servico um banco de VERDADE e nao um
// cache do monolito: o conceito de "gostei desta recomendacao" nasce aqui, o
// monolito nunca fica sabendo dele, e se estes dados sumirem nao ha de onde
// reconstruir. Todo o resto que o microsservico guarda (titulo, capa, nota) e
// derivavel do monolito; isto nao e.
//
// E realimenta o algoritmo: LIKED reforca o genero no perfil de gosto, DISMISSED
// tira o jogo das proximas rodadas pra sempre.
@Entity
@Table(
        name = "recommendation_feedback",
        // Um veredito por jogo por pessoa. Guardar o historico de opinioes daria ao
        // algoritmo sinais contraditorios sobre o mesmo jogo; o que importa e a
        // opiniao atual.
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "game_id"}),
        indexes = @Index(name = "idx_feedback_username", columnList = "username")
)
public class RecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    // EnumType.STRING e nao ORDINAL: gravar 0 e 1 deixaria o banco ilegivel e, se
    // alguem inserisse um valor novo no meio do enum, os dados antigos passariam a
    // significar outra coisa.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackVerdict verdict;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RecommendationFeedback() {
    }

    public RecommendationFeedback(String username, Long gameId, FeedbackVerdict verdict) {
        this.username = username;
        this.gameId = gameId;
        this.verdict = verdict;
        this.createdAt = Instant.now();
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

    public FeedbackVerdict getVerdict() {
        return verdict;
    }

    // Mudar de ideia atualiza o veredito no lugar, em vez de criar outra linha.
    public void setVerdict(FeedbackVerdict verdict) {
        this.verdict = verdict;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Como o algoritmo consome este dado - so o essencial, sem id nem data.
    public FeedbackEntry toEntry() {
        return new FeedbackEntry(gameId, verdict);
    }
}
