package com.gamelog.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.recommendation.domain.FeedbackVerdict;
import com.gamelog.recommendation.domain.RecommendationFeedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// O feedback e o dado EXCLUSIVO deste microsservico: o monolito nao tem conceito
// de "gostei desta recomendacao" e nunca vai saber desse veredito.
//
// E o que justifica o banco proprio ser um banco de verdade, e nao um cache: se
// esta tabela sumir, informacao se perde - nao da pra reconstruir a partir do
// monolito.
@DataJpaTest
class RecommendationFeedbackRepositoryTest {

    @Autowired
    private RecommendationFeedbackRepository feedbackRepository;

    @BeforeEach
    void seed() {
        feedbackRepository.save(new RecommendationFeedback("ana", 1L, FeedbackVerdict.LIKED));
        feedbackRepository.save(new RecommendationFeedback("ana", 2L, FeedbackVerdict.DISMISSED));
    }

    @Test
    @DisplayName("lista todos os vereditos de um usuario")
    void listsEveryVerdictOfAUser() {
        // O algoritmo precisa da lista inteira de uma vez: os LIKED reforcam
        // generos e os DISMISSED excluem candidatos.
        assertThat(feedbackRepository.findByUsername("ana")).hasSize(2);
        assertThat(feedbackRepository.findByUsername("beto")).isEmpty();
    }

    @Test
    @DisplayName("acha o veredito de um jogo especifico")
    void findsTheVerdictForOneGame() {
        assertThat(feedbackRepository.findByUsernameAndGameId("ana", 1L))
                .get()
                .extracting(RecommendationFeedback::getVerdict)
                .isEqualTo(FeedbackVerdict.LIKED);

        assertThat(feedbackRepository.findByUsernameAndGameId("ana", 99L)).isEmpty();
    }

    @Test
    @DisplayName("mudar de ideia atualiza o veredito, nao cria outro")
    void changingYourMindUpdatesTheVerdict() {
        // Uma pessoa pode descartar um jogo e depois curtir. Guardar as duas
        // opinioes deixaria o algoritmo com sinais contraditorios sobre o mesmo
        // jogo - mesmo raciocinio do item de colecao no monolito, que atualiza em
        // vez de duplicar.
        RecommendationFeedback existente =
                feedbackRepository.findByUsernameAndGameId("ana", 2L).orElseThrow();
        existente.setVerdict(FeedbackVerdict.LIKED);
        feedbackRepository.saveAndFlush(existente);

        assertThat(feedbackRepository.findByUsername("ana")).hasSize(2);
        assertThat(feedbackRepository.findByUsernameAndGameId("ana", 2L))
                .get()
                .extracting(RecommendationFeedback::getVerdict)
                .isEqualTo(FeedbackVerdict.LIKED);
    }

    @Test
    @DisplayName("banco impede dois vereditos do mesmo usuario no mesmo jogo")
    void databaseRejectsTwoVerdictsForTheSameGame() {
        assertThatThrownBy(() -> feedbackRepository.saveAndFlush(
                new RecommendationFeedback("ana", 1L, FeedbackVerdict.DISMISSED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
