package com.gamelog.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.dto.GameRatingRow;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User ana;
    private User beto;
    private Game zelda;
    private Game hades;

    @BeforeEach
    void seed() {
        ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        beto = userRepository.save(new User("beto", "beto@email.com", "hash", null));
        zelda = gameRepository.save(new Game(201L, "Zelda", null, 2017, "Aventura", "url"));
        hades = gameRepository.save(new Game(202L, "Hades", null, 2020, "Roguelike", "url"));

        reviewRepository.saveAll(List.of(
                new Review(ana, zelda, 5, "obra prima"),
                new Review(beto, zelda, 3, "bom, mas longo demais"),
                new Review(ana, hades, 4, "viciante")
        ));
    }

    @Test
    void listaReviewsDeUmJogo() {
        List<Review> doZelda = reviewRepository.findByGameIdOrderByCreatedAtDesc(zelda.getId());

        assertThat(doZelda).hasSize(2);
        assertThat(doZelda).allMatch(r -> r.getGame().getId().equals(zelda.getId()));
    }

    @Test
    void listaReviewsDeUmUsuario() {
        List<Review> daAna = reviewRepository.findByUserIdOrderByCreatedAtDesc(ana.getId());

        assertThat(daAna).hasSize(2);
        assertThat(daAna).allMatch(r -> r.getUser().getId().equals(ana.getId()));
    }

    @Test
    void achaReviewPorUsuarioEJogo() {
        assertThat(reviewRepository.findByUserIdAndGameId(ana.getId(), zelda.getId())).isPresent();
        assertThat(reviewRepository.findByUserIdAndGameId(beto.getId(), hades.getId())).isEmpty();
    }

    @Test
    void agregaMediaEContagemNoBanco() {
        // Uma consulta so devolve as estatisticas dos dois jogos.
        List<GameRatingRow> rows = reviewRepository.aggregateByGameIds(
                List.of(zelda.getId(), hades.getId()));

        assertThat(rows).hasSize(2);

        GameRatingRow doZelda = rows.stream()
                .filter(r -> r.gameId().equals(zelda.getId()))
                .findFirst().orElseThrow();
        assertThat(doZelda.average()).isCloseTo(4.0, offset(0.001)); // (5 + 3) / 2
        assertThat(doZelda.count()).isEqualTo(2);

        GameRatingRow doHades = rows.stream()
                .filter(r -> r.gameId().equals(hades.getId()))
                .findFirst().orElseThrow();
        assertThat(doHades.average()).isCloseTo(4.0, offset(0.001));
        assertThat(doHades.count()).isEqualTo(1);
    }

    @Test
    void jogoSemReviewNaoApareceNaAgregacao() {
        Game semReview = gameRepository.save(new Game(203L, "Silksong", null, 2025, "Metroidvania", "url"));

        assertThat(reviewRepository.aggregateByGameIds(List.of(semReview.getId()))).isEmpty();
    }

    @Test
    void bancoImpedeDuasReviewsDoMesmoUsuarioNoMesmoJogo() {
        // Regra de negocio protegida por constraint: (user_id, game_id) unico.
        assertThatThrownBy(() ->
                reviewRepository.saveAndFlush(new Review(ana, zelda, 1, "mudei de ideia")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
