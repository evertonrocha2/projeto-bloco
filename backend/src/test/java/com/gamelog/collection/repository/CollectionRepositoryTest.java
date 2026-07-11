package com.gamelog.collection.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CollectionRepositoryTest {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User davi;
    private Game zelda;
    private Game hades;

    @BeforeEach
    void seed() {
        davi = userRepository.save(new User("davi", "davi@email.com", "hash", null));
        zelda = gameRepository.save(new Game(301L, "Zelda", null, 2017, "Aventura", "url"));
        hades = gameRepository.save(new Game(302L, "Hades", null, 2020, "Roguelike", "url"));

        collectionRepository.saveAll(List.of(
                new CollectionEntry(davi, zelda, 120, "Zerado"),
                new CollectionEntry(davi, hades, 10, "Jogando")
        ));
    }

    @Test
    void listaColecaoDeUmUsuario() {
        List<CollectionEntry> colecao =
                collectionRepository.findByUserIdOrderByCreatedAtDesc(davi.getId());

        assertThat(colecao).hasSize(2);
        assertThat(colecao).allMatch(e -> e.getUser().getId().equals(davi.getId()));
    }

    @Test
    void achaItemPorUsuarioEJogo() {
        var item = collectionRepository.findByUserIdAndGameId(davi.getId(), zelda.getId());

        assertThat(item).isPresent();
        assertThat(item.get().getHoursPlayed()).isEqualTo(120);
        assertThat(item.get().getStatus()).isEqualTo("Zerado");
    }

    @Test
    void atualizaHorasEStatusSemDuplicar() {
        CollectionEntry item = collectionRepository
                .findByUserIdAndGameId(davi.getId(), hades.getId())
                .orElseThrow();

        item.setHoursPlayed(35);
        item.setStatus("Zerado");
        collectionRepository.saveAndFlush(item);

        assertThat(collectionRepository.findByUserIdOrderByCreatedAtDesc(davi.getId())).hasSize(2);
        assertThat(collectionRepository.findByUserIdAndGameId(davi.getId(), hades.getId()))
                .get()
                .extracting(CollectionEntry::getHoursPlayed)
                .isEqualTo(35);
    }
}
