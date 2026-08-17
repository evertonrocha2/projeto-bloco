package com.gamelog.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.domain.ReviewReply;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// Respostas a uma avaliacao, em arvore.
//
// A arvore e montada em MEMORIA a partir de uma consulta plana - por isso o
// repositorio so precisa saber devolver todas as respostas das avaliacoes de uma
// pagina, de uma vez, em ordem. Buscar filho por filho seria uma consulta por no,
// e uma discussao de trinta respostas viraria trinta idas ao banco.
@DataJpaTest
class ReviewReplyRepositoryTest {

    @Autowired
    private ReviewReplyRepository reviewReplyRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User ana;
    private User beto;
    private Review reviewDoZelda;
    private Review reviewDoHades;

    @BeforeEach
    void seed() {
        ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        beto = userRepository.save(new User("beto", "beto@email.com", "hash", null));

        Game zelda = gameRepository.save(new Game(701L, "Zelda", null, 2017, "Aventura", "url"));
        Game hades = gameRepository.save(new Game(702L, "Hades", null, 2020, "Roguelike", "url"));

        reviewDoZelda = reviewRepository.save(new Review(ana, zelda, 5, "obra prima"));
        reviewDoHades = reviewRepository.save(new Review(ana, hades, 4, "viciante"));
    }

    @Test
    void guardaUmaRespostaDeRaiz() {
        ReviewReply resposta = reviewReplyRepository.save(
                new ReviewReply(reviewDoZelda, beto, null, "discordo do final"));

        assertThat(resposta.getParent()).isNull();
        // Raiz e nivel 0. A conta de profundidade parte daqui.
        assertThat(resposta.getDepth()).isZero();
    }

    @Test
    void umaRespostaApontaPraOutra() {
        ReviewReply raiz = reviewReplyRepository.save(
                new ReviewReply(reviewDoZelda, beto, null, "discordo do final"));
        ReviewReply filha = reviewReplyRepository.save(
                new ReviewReply(reviewDoZelda, ana, raiz, "o final e o melhor"));

        assertThat(filha.getParent().getId()).isEqualTo(raiz.getId());
        assertThat(filha.getDepth()).isEqualTo(1);
    }

    @Test
    void traRespostasDeVariasAvaliacoesNumaConsultaSo() {
        // E o que a pagina de um jogo pede: todas as respostas de todas as
        // avaliacoes que ela vai mostrar, de uma vez.
        reviewReplyRepository.saveAll(List.of(
                new ReviewReply(reviewDoZelda, beto, null, "discordo"),
                new ReviewReply(reviewDoZelda, ana, null, "concordo"),
                new ReviewReply(reviewDoHades, beto, null, "platinei em 80h")
        ));

        List<ReviewReply> todas = reviewReplyRepository.findByReviewIdInOrderByCreatedAtAsc(
                List.of(reviewDoZelda.getId(), reviewDoHades.getId()));

        assertThat(todas).hasSize(3);
    }

    @Test
    void naoTrazRespostaDeAvaliacaoForaDaPagina() {
        reviewReplyRepository.saveAll(List.of(
                new ReviewReply(reviewDoZelda, beto, null, "discordo"),
                new ReviewReply(reviewDoHades, beto, null, "platinei em 80h")
        ));

        List<ReviewReply> soDoZelda = reviewReplyRepository.findByReviewIdInOrderByCreatedAtAsc(
                List.of(reviewDoZelda.getId()));

        assertThat(soDoZelda).hasSize(1);
        assertThat(soDoZelda.get(0).getText()).isEqualTo("discordo");
    }

    @Test
    void sabeSeUmaRespostaTemFilhas() {
        // E a pergunta que decide entre apagar de verdade e virar lapide: sumir
        // com uma resposta que tem filhas levaria o galho inteiro junto.
        ReviewReply comFilha = reviewReplyRepository.save(
                new ReviewReply(reviewDoZelda, beto, null, "discordo"));
        ReviewReply semFilha = reviewReplyRepository.save(
                new ReviewReply(reviewDoZelda, beto, null, "sozinha"));
        reviewReplyRepository.save(new ReviewReply(reviewDoZelda, ana, comFilha, "por que?"));

        assertThat(reviewReplyRepository.existsByParentId(comFilha.getId())).isTrue();
        assertThat(reviewReplyRepository.existsByParentId(semFilha.getId())).isFalse();
    }

    @Test
    void aLapidePerdeOTextoMasContinuaNaArvore() {
        ReviewReply resposta = reviewReplyRepository.save(
                new ReviewReply(reviewDoZelda, beto, null, "me arrependi de escrever"));

        resposta.tombstone();
        reviewReplyRepository.saveAndFlush(resposta);

        ReviewReply relida = reviewReplyRepository.findById(resposta.getId()).orElseThrow();
        assertThat(relida.getText()).isNull();
        assertThat(relida.isDeleted()).isTrue();
    }
}
