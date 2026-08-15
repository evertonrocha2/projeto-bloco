package com.gamelog.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.List;

// Uma recomendacao gravada: "para o usuario X, o jogo Y, com esta pontuacao e por
// este motivo".
//
// === A fronteira do microsservico mora aqui ===
//
// Repare que nao existe @ManyToOne pra User nem pra Game. Nao e esquecimento: sao
// entidades de OUTRO servico, com outro banco. A ligacao se faz por username e
// gameId - identificadores, nao referencias. Uma chave estrangeira aqui exigiria
// que as duas tabelas vivessem no mesmo banco, e ai nao haveria dois servicos, e
// sim uma aplicacao com dois processos e um banco compartilhado.
//
// O preco dessa autonomia e nao poder pedir ao banco pra garantir que o gameId
// existe. E um preco consciente: em troca, o microsservico continua respondendo
// quando o monolito cai, e cada lado pode mudar o proprio esquema sem avisar o
// outro.
@Entity
@Table(
        name = "recommendations",
        // Uma recomendacao por jogo por usuario. Sem isso, um recalculo que falhasse
        // no meio poderia deixar o mesmo jogo repetido na tela.
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "game_id"}),
        // Toda consulta deste repositorio filtra por username; sem indice o banco
        // varreria a tabela inteira a cada abertura da tela.
        indexes = @Index(name = "idx_recommendations_username", columnList = "username")
)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identidade do usuario no outro servico. String e nao FK, de proposito.
    @Column(nullable = false)
    private String username;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    // Titulo e capa vem DUPLICADOS do catalogo, de proposito.
    //
    // Sem isso, montar a tela exigiria uma segunda chamada ao monolito pra
    // descobrir o nome de cada jogo - e, pior, o modo degradado nao funcionaria:
    // com o monolito fora do ar, a resposta seria uma lista de ids, inutil pro
    // usuario. Duplicar o dado no momento da geracao e o preco normal de
    // autonomia entre servicos.
    @Column(name = "game_title", nullable = false)
    private String gameTitle;

    @Column(name = "game_cover_url", length = 1000)
    private String gameCoverUrl;

    @Column(nullable = false)
    private double score;

    // Generos que justificam a recomendacao, guardados como "RPG,Action".
    //
    // Podia ser uma tabela filha, mas seria uma tabela inteira pra guardar no
    // maximo dois rotulos que nunca sao consultados isoladamente - so lidos junto
    // com a recomendacao. Vazio significa "veio da nota da comunidade".
    @Column(name = "reason_genres", length = 500)
    private String reasonGenres;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected Recommendation() {
    }

    public Recommendation(String username, Long gameId, String gameTitle, String gameCoverUrl,
                          double score, String reasonGenres, Instant generatedAt) {
        this.username = username;
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.gameCoverUrl = gameCoverUrl;
        this.score = score;
        this.reasonGenres = reasonGenres;
        this.generatedAt = generatedAt;
    }

    // Converte o resultado do algoritmo em linha do banco. A juncao dos generos
    // acontece aqui pra que o motor de recomendacao continue trabalhando com
    // List<String> e nao saiba nada sobre como o dado e armazenado.
    public static Recommendation from(String username, ScoredGame scored, Instant generatedAt) {
        return new Recommendation(
                username,
                scored.gameId(),
                scored.title(),
                scored.coverUrl(),
                scored.score(),
                String.join(",", scored.reasonGenres()),
                generatedAt);
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

    public String getGameTitle() {
        return gameTitle;
    }

    public String getGameCoverUrl() {
        return gameCoverUrl;
    }

    public double getScore() {
        return score;
    }

    // Devolve os generos de volta como lista, pra API nao expor o detalhe de que
    // eles foram guardados numa string separada por virgula.
    public List<String> getReasonGenreList() {
        if (reasonGenres == null || reasonGenres.isBlank()) {
            return List.of();
        }
        return List.of(reasonGenres.split(","));
    }

    public String getReasonGenres() {
        return reasonGenres;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
