package com.gamelog.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.domain.ReviewVote;
import com.gamelog.review.domain.VoteType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// Votar numa avaliacao alheia. Duas coisas precisam ser verdade no banco, e nao
// so no service: uma pessoa tem no maximo UM voto por avaliacao, e as contagens
// da pagina de um jogo saem numa consulta so.
//
// A segunda importa mais do que parece. A pagina de um jogo lista N avaliacoes;
// contar positivos e negativos avaliacao por avaliacao seriam 2N consultas pra
// desenhar uma tela. A agregacao aqui e o que evita isso - mesmo caminho que o
// countByStatusForUser da colecao ja tinha tomado.
@DataJpaTest
class ReviewVoteRepositoryTest {

    @Autowired
    private ReviewVoteRepository reviewVoteRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User ana;
    private User beto;
    private User carla;
    private Review reviewDoZelda;
    private Review reviewDoHades;

    @BeforeEach
    void seed() {
        ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        beto = userRepository.save(new User("beto", "beto@email.com", "hash", null));
        carla = userRepository.save(new User("carla", "carla@email.com", "hash", null));

        Game zelda = gameRepository.save(new Game(601L, "Zelda", null, 2017, "Aventura", "url"));
        Game hades = gameRepository.save(new Game(602L, "Hades", null, 2020, "Roguelike", "url"));

        reviewDoZelda = reviewRepository.save(new Review(ana, zelda, 5, "obra prima"));
        reviewDoHades = reviewRepository.save(new Review(ana, hades, 4, "viciante"));
    }

    @Test
    void guardaUmVoto() {
        reviewVoteRepository.save(new ReviewVote(beto, reviewDoZelda, VoteType.POSITIVE));

        assertThat(reviewVoteRepository.findByUserIdAndReviewId(beto.getId(), reviewDoZelda.getId()))
                .isPresent()
                .get()
                .extracting(ReviewVote::getType)
                .isEqualTo(VoteType.POSITIVE);
    }

    @Test
    void recusaDoisVotosDamesmaPessoaNaMesmaAvaliacao() {
        // A constraint e a ultima linha de defesa: mesmo que o service falhe na
        // checagem - dois cliques rapidos, duas requisicoes simultaneas - o banco
        // nao deixa o mesmo par (usuario, avaliacao) entrar duas vezes.
        reviewVoteRepository.save(new ReviewVote(beto, reviewDoZelda, VoteType.POSITIVE));

        assertThatThrownBy(() -> reviewVoteRepository.saveAndFlush(
                new ReviewVote(beto, reviewDoZelda, VoteType.NEGATIVE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aMesmaPessoaVotaEmAvaliacoesDiferentes() {
        reviewVoteRepository.save(new ReviewVote(beto, reviewDoZelda, VoteType.POSITIVE));
        reviewVoteRepository.saveAndFlush(new ReviewVote(beto, reviewDoHades, VoteType.NEGATIVE));

        assertThat(reviewVoteRepository.findAll()).hasSize(2);
    }

    @Test
    void contaPositivosENegativosDeVariasAvaliacoesNumaConsultaSo() {
        reviewVoteRepository.saveAll(List.of(
                new ReviewVote(beto, reviewDoZelda, VoteType.POSITIVE),
                new ReviewVote(carla, reviewDoZelda, VoteType.POSITIVE),
                new ReviewVote(beto, reviewDoHades, VoteType.NEGATIVE)
        ));

        List<Object[]> linhas = reviewVoteRepository.countByTypeForReviews(
                List.of(reviewDoZelda.getId(), reviewDoHades.getId()));

        // Cada linha e (reviewId, tipo, total). Duas avaliacoes com votos de tipos
        // diferentes produzem tres agrupamentos - dois positivos do Zelda somados
        // numa linha, um negativo do Hades noutra.
        Map<String, Long> porChave = linhas.stream().collect(Collectors.toMap(
                linha -> linha[0] + ":" + linha[1],
                linha -> (Long) linha[2]));

        assertThat(porChave)
                .containsEntry(reviewDoZelda.getId() + ":POSITIVE", 2L)
                .containsEntry(reviewDoHades.getId() + ":NEGATIVE", 1L)
                .hasSize(2);
    }

    @Test
    void naoContaVotoDeAvaliacaoForaDaPagina() {
        // A pagina pede as contagens SO das avaliacoes que ela mostra. Se a
        // consulta ignorasse o filtro, a primeira pagina traria o banco inteiro.
        reviewVoteRepository.saveAll(List.of(
                new ReviewVote(beto, reviewDoZelda, VoteType.POSITIVE),
                new ReviewVote(beto, reviewDoHades, VoteType.NEGATIVE)
        ));

        List<Object[]> linhas = reviewVoteRepository.countByTypeForReviews(
                List.of(reviewDoZelda.getId()));

        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0)[0]).isEqualTo(reviewDoZelda.getId());
    }

    @Test
    void buscaOsMeusVotosEmLote() {
        // A tela precisa saber em quais avaliacoes EU ja votei, pra marcar o
        // polegar. Tambem em lote, pelo mesmo motivo das contagens.
        reviewVoteRepository.saveAll(List.of(
                new ReviewVote(beto, reviewDoZelda, VoteType.POSITIVE),
                new ReviewVote(carla, reviewDoHades, VoteType.NEGATIVE)
        ));

        List<ReviewVote> doBeto = reviewVoteRepository.findByUserIdAndReviewIdIn(
                beto.getId(), List.of(reviewDoZelda.getId(), reviewDoHades.getId()));

        assertThat(doBeto).hasSize(1);
        assertThat(doBeto.get(0).getReview().getId()).isEqualTo(reviewDoZelda.getId());
    }
}
