package com.gamelog.recommendation.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// Um jogo do catalogo, na copia local que este servico mantem.
//
// === Projecao (read model) ===
//
// Ate o TP3 o catalogo era buscado no monolito a cada recalculo. Agora ele vive
// aqui tambem, montado a partir dos eventos catalog.game.added e do snapshot
// inicial. O dono do dado continua sendo o monolito - esta tabela e uma COPIA
// derivada, que pode ser apagada e reconstruida pedindo um snapshot novo.
//
// O id e o mesmo do monolito (nao e gerado aqui). E a chave de juncao com as
// notas e a colecao, que chegam referenciando o jogo por esse id.
@Entity
@Table(name = "catalog_games")
public class CatalogGameView {

    @Id
    @Column(name = "game_id")
    private Long gameId;

    @Column(nullable = false)
    private String title;

    private String genre;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogGameView() {
    }

    public CatalogGameView(Long gameId, String title, String genre, String coverUrl, Instant updatedAt) {
        this.gameId = gameId;
        this.title = title;
        this.genre = genre;
        this.coverUrl = coverUrl;
        this.updatedAt = updatedAt;
    }

    // Um evento de review traz titulo e genero, mas nao a capa. Se o jogo ja e
    // conhecido, a capa gravada nao pode ser apagada por um evento que nao a tem.
    public void refresh(String title, String genre, String coverUrl, Instant when) {
        this.title = title;
        this.genre = genre;
        if (coverUrl != null) {
            this.coverUrl = coverUrl;
        }
        this.updatedAt = when;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
