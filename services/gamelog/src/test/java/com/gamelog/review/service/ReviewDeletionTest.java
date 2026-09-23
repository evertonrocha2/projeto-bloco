package com.gamelog.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.messaging.EventTypes;
import com.gamelog.messaging.RecordingEventPublisher;
import com.gamelog.review.domain.Review;
import com.gamelog.review.domain.VoteType;
import com.gamelog.review.dto.CreateReplyRequest;
import com.gamelog.review.dto.ReplyResponse;
import com.gamelog.review.repository.ReviewReplyRepository;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.review.repository.ReviewVoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// Apagar uma review que ja tem conversa.
//
// Achado no TP5, quando o monolito passou a rodar em PostgreSQL no docker compose:
// o smoke test apagava a review do teste e recebia 403. Por baixo, a chave
// estrangeira review_replies -> reviews recusava o DELETE, o 500 ia pro /error e
// o /error exige autenticacao. Nenhum teste apagava review COM resposta ou voto -
// em H2 o erro seria o mesmo; so faltava o caso.
@DataJpaTest
class ReviewDeletionTest {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ReviewReplyRepository replyRepository;
    @Autowired private ReviewVoteRepository voteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void apagarReviewLevaJuntoRespostasAninhadasEVotos() {
        RecordingEventPublisher events = new RecordingEventPublisher();
        ReviewService reviews = new ReviewService(reviewRepository, userRepository, gameRepository,
                replyRepository, voteRepository, events);
        ReviewSocialService social = new ReviewSocialService(
                voteRepository, replyRepository, reviewRepository, userRepository, events);

        User ana = userRepository.save(new User("ana", "ana@email.com", "hash", null));
        userRepository.save(new User("beto", "beto@email.com", "hash", null));
        Game zelda = gameRepository.save(new Game(801L, "Zelda", null, 2017, "Aventura", "url"));
        Review review = reviewRepository.save(new Review(ana, zelda, 5, "obra prima"));

        ReplyResponse raiz = social.reply("beto", review.getId(), new CreateReplyRequest("concordo", null));
        social.reply("ana", review.getId(), new CreateReplyRequest("valeu!", raiz.id()));
        social.vote("beto", review.getId(), VoteType.POSITIVE);
        entityManager.flush();

        reviews.delete("ana", review.getId());
        // O flush e onde o banco confere as chaves estrangeiras; sem ele o teste
        // passaria sem o DELETE ter chegado ao banco.
        entityManager.flush();

        assertThat(reviewRepository.findById(review.getId())).isEmpty();
        assertThat(replyRepository.findAll()).isEmpty();
        assertThat(voteRepository.findAll()).isEmpty();
        assertThat(events.types()).contains(EventTypes.REVIEW_DELETED);
    }
}
