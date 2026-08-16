package com.gamelog.collection.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.collection.domain.CollectionEntry;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.history.Revisions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// Historico da colecao: o caso de uso classico e acompanhar a jornada com um
// jogo ("adicionei querendo jogar -> comecei -> zerei"). Cada transacao de
// update vira uma revisao. Ver o comentario no ReviewHistoryTest sobre por que
// o teste desliga a propria transacao e commita na mao.
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CollectionHistoryTest {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
    }

    @Test
    void guardaALinhaDoTempoDoItemDaColecao() {
        // Adiciona o jogo na colecao so com a intencao de jogar...
        Long entryId = tx.execute(status -> {
            User user = userRepository.save(new User("helena", "helena@email.com", "hash", null));
            Game game = gameRepository.save(new Game(501L, "Baldur's Gate 3", null, 2023, "RPG", "url"));
            return collectionRepository.save(
                    new CollectionEntry(user, game, 0, "Quero jogar")).getId();
        });

        // ...depois comeca a jogar...
        tx.executeWithoutResult(status -> {
            CollectionEntry entry = collectionRepository.findById(entryId).orElseThrow();
            entry.setHoursPlayed(30);
            entry.setStatus("Jogando");
            collectionRepository.save(entry);
        });

        // ...e por fim zera.
        tx.executeWithoutResult(status -> {
            CollectionEntry entry = collectionRepository.findById(entryId).orElseThrow();
            entry.setHoursPlayed(112);
            entry.setStatus("Zerado");
            collectionRepository.save(entry);
        });

        Revisions<Long, CollectionEntry> revisions = collectionRepository.findRevisions(entryId);

        assertThat(revisions.getContent()).hasSize(3);

        var statusAoLongoDoTempo = revisions.getContent().stream()
                .map(r -> r.getEntity().getStatus())
                .toList();
        assertThat(statusAoLongoDoTempo)
                .containsExactly("Quero jogar", "Jogando", "Zerado");

        var horasAoLongoDoTempo = revisions.getContent().stream()
                .map(r -> r.getEntity().getHoursPlayed())
                .toList();
        assertThat(horasAoLongoDoTempo).containsExactly(0, 30, 112);

        // A revisao mais recente reflete o estado atual.
        var ultima = collectionRepository.findLastChangeRevision(entryId).orElseThrow();
        assertThat(ultima.getEntity().getStatus()).isEqualTo("Zerado");
    }
}
