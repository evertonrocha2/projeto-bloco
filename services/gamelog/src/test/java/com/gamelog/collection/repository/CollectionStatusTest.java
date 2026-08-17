package com.gamelog.collection.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.collection.domain.CollectionStatus;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// O status da colecao virou enum. Antes era String livre validada so com
// @NotBlank, o que aceitava qualquer texto - inclusive erro de digitacao, que
// criaria uma "lista" fantasma que nenhuma tela sabe exibir.
//
// Estes testes cobrem as consultas que as novas telas precisam: wishlist,
// zerados e platinas sao a MESMA tabela filtrada por status diferente.
@DataJpaTest
class CollectionStatusTest {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User ana;

    @BeforeEach
    void seed() {
        ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        User beto = userRepository.save(new User("beto", "beto@email.com", "hash", null));

        Game zelda = gameRepository.save(new Game(501L, "Zelda", null, 2017, "Aventura", "url"));
        Game hades = gameRepository.save(new Game(502L, "Hades", null, 2020, "Roguelike", "url"));
        Game elden = gameRepository.save(new Game(503L, "Elden Ring", null, 2022, "RPG", "url"));
        Game hollow = gameRepository.save(new Game(504L, "Hollow Knight", null, 2017, "Indie", "url"));

        collectionRepository.saveAll(List.of(
                new CollectionEntry(ana, zelda, 120, CollectionStatus.PLATINADO),
                new CollectionEntry(ana, hades, 40, CollectionStatus.ZERADO),
                new CollectionEntry(ana, elden, 0, CollectionStatus.QUERO_JOGAR),
                new CollectionEntry(beto, hollow, 10, CollectionStatus.PLATINADO)));
    }

    @Test
    @DisplayName("a wishlist e a colecao filtrada por QUERO_JOGAR")
    void wishlistIsTheCollectionFilteredByWantToPlay() {
        List<CollectionEntry> wishlist =
                collectionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        ana.getId(), CollectionStatus.QUERO_JOGAR);

        assertThat(wishlist).singleElement()
                .extracting(entry -> entry.getGame().getTitle())
                .isEqualTo("Elden Ring");
    }

    @Test
    @DisplayName("platinados sao separados de zerados")
    void platinumIsSeparateFromFinished() {
        // Platinar nao e "zerar com enfeite": sao conquistas diferentes, e quem
        // coleciona quer ver as duas listas separadas.
        List<CollectionEntry> platinados =
                collectionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        ana.getId(), CollectionStatus.PLATINADO);
        List<CollectionEntry> zerados =
                collectionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        ana.getId(), CollectionStatus.ZERADO);

        assertThat(platinados).singleElement()
                .extracting(entry -> entry.getGame().getTitle()).isEqualTo("Zelda");
        assertThat(zerados).singleElement()
                .extracting(entry -> entry.getGame().getTitle()).isEqualTo("Hades");
    }

    @Test
    @DisplayName("filtra por status sem misturar usuarios")
    void filteringByStatusDoesNotMixUsers() {
        // Os dois tem um jogo platinado; cada um so pode ver o proprio.
        assertThat(collectionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                ana.getId(), CollectionStatus.PLATINADO)).hasSize(1);
    }

    @Test
    @DisplayName("conta quantos jogos ha em cada status, numa consulta so")
    void countsGamesPerStatusInOneQuery() {
        // As abas mostram o total ao lado do nome. Sem essa agregacao seria uma
        // consulta por aba - cinco idas ao banco pra montar um cabecalho.
        List<Object[]> contagem = collectionRepository.countByStatusForUser(ana.getId());

        assertThat(contagem).hasSize(3);
    }

    @Test
    @DisplayName("o rotulo de exibicao nao vaza pro banco")
    void displayLabelIsNotWhatGetsStored() {
        // Guardamos o NOME do enum (QUERO_JOGAR) e exibimos o rotulo
        // ("Quero jogar"), entao reescrever o texto da tela nao exige migrar dados.
        assertThat(CollectionStatus.QUERO_JOGAR.getLabel()).isEqualTo("Quero jogar");
        assertThat(CollectionStatus.QUERO_JOGAR.name()).isEqualTo("QUERO_JOGAR");
    }
}
