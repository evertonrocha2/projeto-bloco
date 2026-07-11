package com.gamelog.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.shared.audit.AuditRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.Revisions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// Teste do historico (Envers). Detalhe importante: o Envers so grava a revisao
// no COMMIT da transacao. O @DataJpaTest normal roda tudo numa transacao que e
// desfeita no final - nenhuma revisao seria gravada. Por isso a gente desliga
// a transacao do teste (NOT_SUPPORTED) e abre transacoes reais na mao com o
// TransactionTemplate, uma por operacao, igual acontece em producao (cada
// requisicao HTTP = uma transacao = uma revisao).
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReviewHistoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

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
    void registraCriacaoEdicaoEExclusaoNoHistorico() {
        // 1a transacao: cria a review (nota 5).
        Long reviewId = tx.execute(status -> {
            User user = userRepository.save(new User("franca", "franca@email.com", "hash", null));
            Game game = gameRepository.save(new Game(401L, "Hollow Knight", null, 2017, "Metroidvania", "url"));
            return reviewRepository.save(new Review(user, game, 5, "perfeito")).getId();
        });

        // 2a transacao: edita (nota 3).
        tx.executeWithoutResult(status -> {
            Review review = reviewRepository.findById(reviewId).orElseThrow();
            review.update(3, "revi minha opiniao depois do final verdadeiro");
            reviewRepository.save(review);
        });

        // 3a transacao: apaga.
        tx.executeWithoutResult(status ->
                reviewRepository.deleteById(reviewId));

        // O historico guarda a vida inteira da review, mesmo ela ja apagada.
        Revisions<Long, Review> revisions = reviewRepository.findRevisions(reviewId);

        assertThat(revisions.getContent()).hasSize(3);

        var tipos = revisions.getContent().stream()
                .map(r -> r.getMetadata().getRevisionType())
                .toList();
        assertThat(tipos).containsExactly(
                RevisionMetadata.RevisionType.INSERT,
                RevisionMetadata.RevisionType.UPDATE,
                RevisionMetadata.RevisionType.DELETE);

        // Cada revisao carrega a "foto" da entidade naquele momento.
        assertThat(revisions.getContent().get(0).getEntity().getRating()).isEqualTo(5);
        assertThat(revisions.getContent().get(1).getEntity().getRating()).isEqualTo(3);
        assertThat(revisions.getContent().get(1).getEntity().getText())
                .contains("revi minha opiniao");

        // store_data_at_delete=true: ate a revisao de DELETE guarda o estado final.
        assertThat(revisions.getContent().get(2).getEntity().getRating()).isEqualTo(3);
    }

    @Test
    void revisaoGuardaQuemFezEQuando() {
        Long reviewId = tx.execute(status -> {
            User user = userRepository.save(new User("gabi", "gabi@email.com", "hash", null));
            Game game = gameRepository.save(new Game(402L, "Celeste", null, 2018, "Plataforma", "url"));
            return reviewRepository.save(new Review(user, game, 4, "dificil e lindo")).getId();
        });

        var revision = reviewRepository.findLastChangeRevision(reviewId).orElseThrow();
        AuditRevision meta = (AuditRevision) revision.getMetadata().getDelegate();

        // Sem ninguem autenticado (caso dos testes e do seeder), o listener
        // registra "sistema". Numa requisicao real viria o username do token.
        assertThat(meta.getUsername()).isEqualTo("sistema");
        assertThat(meta.getInstant()).isNotNull();
        assertThat(revision.getRequiredRevisionNumber()).isPositive();
    }
}
