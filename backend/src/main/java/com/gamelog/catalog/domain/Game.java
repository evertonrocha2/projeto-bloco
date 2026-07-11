package com.gamelog.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Um jogo do catalogo. E sobre ele que os usuarios escrevem reviews.
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id do jogo la na API externa (RAWG). Guardamos pra conseguir buscar a
    // descricao completa sob demanda depois (lazy load), sem precisar baixar
    // tudo de uma vez no startup.
    private Long externalId;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    private Integer releaseYear;

    // Generos / "topicos" do jogo, ja juntos numa string (ex: "Action, RPG").
    private String genre;

    // URL da capa do jogo (a gente usa a imagem que vem da API externa).
    @Column(length = 1000)
    private String coverUrl;

    protected Game() {
    }

    public Game(Long externalId, String title, String description, Integer releaseYear, String genre, String coverUrl) {
        this.externalId = externalId;
        this.title = title;
        this.description = description;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.coverUrl = coverUrl;
    }

    public Long getId() {
        return id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    // A descricao pode ser preenchida depois, na primeira vez que alguem abre o
    // jogo - ai a gente busca na API externa e salva pra nao buscar de novo.
    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public String getGenre() {
        return genre;
    }

    public String getCoverUrl() {
        return coverUrl;
    }
}
