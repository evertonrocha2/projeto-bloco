package com.gamelog.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.recommendation.domain.Recommendation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// Testes do repositorio de recomendacoes - a camada de persistencia PROPRIA do
// microsservico.
//
// Vale reparar no que NAO aparece aqui: nenhum User, nenhum Game, nenhum
// repositorio do monolito. As linhas desta tabela referenciam usuario e jogo so
// por username e gameId, sem chave estrangeira. E isso que permite os dois
// servicos terem bancos separados e evoluirem sem se coordenar.
@DataJpaTest
class RecommendationRepositoryTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    private Instant agora;

    @BeforeEach
    void seed() {
        agora = Instant.now();

        recommendationRepository.saveAll(List.of(
                new Recommendation("ana", 1L, "Elden Ring", "url1", 3.3, "RPG", agora),
                new Recommendation("ana", 3L, "Hades", "url3", 1.92, "", agora),
                new Recommendation("ana", 4L, "Celeste", "url4", 4.10, "Indie", agora),
                new Recommendation("beto", 2L, "FIFA", "url2", 1.2, "Sports", agora)));
    }

    @Test
    @DisplayName("lista as recomendacoes da maior pra menor pontuacao")
    void listsRecommendationsFromHighestScore() {
        // A ordenacao acontece no banco, nao em memoria: a tela mostra a lista
        // exatamente nessa ordem e nao precisa reordenar nada.
        List<Recommendation> daAna = recommendationRepository.findByUsernameOrderByScoreDesc("ana");

        assertThat(daAna).extracting(Recommendation::getGameId)
                .containsExactly(4L, 1L, 3L);
    }

    @Test
    @DisplayName("nao mistura recomendacoes de usuarios diferentes")
    void doesNotMixUsers() {
        assertThat(recommendationRepository.findByUsernameOrderByScoreDesc("beto"))
                .hasSize(1)
                .allMatch(rec -> rec.getUsername().equals("beto"));
    }

    @Test
    @DisplayName("recalcular substitui o lote sem duplicar")
    void refreshReplacesTheBatchWithoutDuplicating() {
        // Recalcular apaga o lote antigo e grava o novo. Se so gravasse, cada
        // recalculo somaria linhas e a tela mostraria o mesmo jogo varias vezes -
        // e a constraint de unicidade estouraria na segunda rodada.
        recommendationRepository.deleteByUsername("ana");
        recommendationRepository.saveAndFlush(
                new Recommendation("ana", 1L, "Elden Ring", "url1", 3.5, "RPG", agora));

        assertThat(recommendationRepository.findByUsernameOrderByScoreDesc("ana")).hasSize(1);
        // Apagar o lote da ana nao pode afetar o do beto.
        assertThat(recommendationRepository.findByUsernameOrderByScoreDesc("beto")).hasSize(1);
    }

    @Test
    @DisplayName("informa se o usuario ja tem um lote gravado")
    void tellsWhetherUserHasABatch() {
        // Usado pra decidir entre servir o que ja existe e gerar na hora.
        assertThat(recommendationRepository.existsByUsername("ana")).isTrue();
        assertThat(recommendationRepository.existsByUsername("ninguem")).isFalse();
    }

    @Test
    @DisplayName("banco impede o mesmo jogo duas vezes para o mesmo usuario")
    void databaseRejectsDuplicateGameForSameUser() {
        // Regra protegida por constraint, nao so por codigo: se um bug no service
        // tentasse gravar duplicado, o banco recusa.
        assertThatThrownBy(() -> recommendationRepository.saveAndFlush(
                new Recommendation("ana", 1L, "Elden Ring", "url1", 9.9, "RPG", agora)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
