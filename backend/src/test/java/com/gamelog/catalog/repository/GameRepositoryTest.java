package com.gamelog.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @BeforeEach
    void seed() {
        gameRepository.saveAll(List.of(
                new Game(101L, "The Legend of Zelda", null, 2017, "Aventura", "url1"),
                new Game(102L, "Zelda: Tears of the Kingdom", null, 2023, "Aventura", "url2"),
                new Game(103L, "Elden Ring", null, 2022, "RPG", "url3"),
                new Game(null, "Hades", null, 2020, "Roguelike", "url4")
        ));
    }

    @Test
    void buscaPorTituloIgnorandoMaiusculas() {
        Page<Game> page = gameRepository.findByTitleContainingIgnoreCase(
                "zelda", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allMatch(game -> game.getTitle().toLowerCase().contains("zelda"));
    }

    @Test
    void paginacaoLimitaOsResultadosEInformaOTotal() {
        // Pede paginas de tamanho 2: o banco devolve so 2 jogos por vez,
        // mas informa que existem 4 no total (2 paginas).
        Page<Game> primeira = gameRepository.findByTitleContainingIgnoreCase(
                "", PageRequest.of(0, 2, Sort.by("title")));

        assertThat(primeira.getContent()).hasSize(2);
        assertThat(primeira.getTotalElements()).isEqualTo(4);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.hasNext()).isTrue();
    }

    @Test
    void achaJogoPeloIdExterno() {
        assertThat(gameRepository.findByExternalId(103L))
                .isPresent()
                .get()
                .extracting(Game::getTitle)
                .isEqualTo("Elden Ring");

        assertThat(gameRepository.findByExternalId(999L)).isEmpty();
    }
}
