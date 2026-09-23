package com.gamelog.review.service;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.messaging.EventPublisher;
import com.gamelog.messaging.EventTypes;
import com.gamelog.messaging.event.ReviewEventPayload;
import com.gamelog.review.domain.Review;
import com.gamelog.review.dto.CreateReviewRequest;
import com.gamelog.review.dto.GameRatingRow;
import com.gamelog.review.dto.RatingStats;
import com.gamelog.review.dto.ReviewRevisionResponse;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Concentra tudo que envolve reviews: criar, editar, apagar, listar, calcular
// medias e consultar o historico de mudancas. Como uma review liga usuario e
// jogo, esse service conversa com os tres repositorios.
//
// TP4: toda escrita tambem anuncia o fato (review.created/updated/deleted). O
// evento e gravado na MESMA transacao da review (outbox), entao nao existe review
// sem evento nem evento sem review. Este service nao sabe quem vai ouvir - antes do
// TP4 era o microsservico que vinha perguntar; agora o monolito so conta o que
// aconteceu.
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final EventPublisher eventPublisher;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         GameRepository gameRepository,
                         EventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReviewResponse create(String username, Long gameId, CreateReviewRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado"));

        // Regra de negocio: uma review por pessoa por jogo.
        reviewRepository.findByUserIdAndGameId(user.getId(), gameId).ifPresent(existing -> {
            throw new BadRequestException("Voce ja avaliou esse jogo");
        });

        // Defesa extra da regra da nota (a validacao do DTO ja cobre, mas o
        // service nao confia cegamente em quem o chamou).
        if (request.rating() < 0 || request.rating() > 5) {
            throw new BadRequestException("A nota deve estar entre 0 e 5");
        }

        Review review = new Review(user, game, request.rating(), request.text());
        reviewRepository.save(review);
        eventPublisher.publish(EventTypes.REVIEW_CREATED, String.valueOf(review.getId()),
                ReviewEventPayload.from(review));
        return ReviewResponse.from(review);
    }

    // Editar review: so o autor pode, e so nota e texto mudam. O Envers grava
    // automaticamente uma revisao UPDATE com o estado novo - e assim que o
    // historico "acontece", sem nenhum codigo extra aqui.
    @Transactional
    public ReviewResponse update(String username, Long reviewId, CreateReviewRequest request) {
        Review review = findOwnedReview(username, reviewId);

        if (request.rating() < 0 || request.rating() > 5) {
            throw new BadRequestException("A nota deve estar entre 0 e 5");
        }

        review.update(request.rating(), request.text());
        reviewRepository.save(review);
        eventPublisher.publish(EventTypes.REVIEW_UPDATED, String.valueOf(review.getId()),
                ReviewEventPayload.from(review));
        return ReviewResponse.from(review);
    }

    // Apagar review: o Envers registra uma revisao DELETE, entao mesmo apagada
    // a review continua consultavel no historico (trilha de auditoria).
    @Transactional
    public void delete(String username, Long reviewId) {
        Review review = findOwnedReview(username, reviewId);
        // O payload e montado ANTES do delete: depois dele a review nao pode mais
        // ser lida, e o consumidor precisa saber qual nota tirar da conta.
        ReviewEventPayload lastState = ReviewEventPayload.from(review);
        reviewRepository.delete(review);
        eventPublisher.publish(EventTypes.REVIEW_DELETED, String.valueOf(reviewId), lastState);
    }

    // Linha do tempo de uma review: cada revisao traz o estado daquele momento
    // mais quem mudou e quando. Vem do RevisionRepository (Envers).
    @Transactional(readOnly = true)
    public List<ReviewRevisionResponse> history(String username, Long reviewId) {
        findOwnedReview(username, reviewId);
        return reviewRepository.findRevisions(reviewId).stream()
                .map(ReviewRevisionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByGame(Long gameId) {
        return reviewRepository.findByGameIdOrderByCreatedAtDesc(gameId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // Media e contagem de UM jogo, calculadas pelo banco (AVG/COUNT).
    @Transactional(readOnly = true)
    public RatingStats statsForGame(Long gameId) {
        return statsForGames(List.of(gameId)).getOrDefault(gameId, RatingStats.empty());
    }

    // Media e contagem de VARIOS jogos numa consulta so. O catalogo usa isso
    // pra montar todos os cards sem cair no problema N+1 (um SELECT por jogo).
    @Transactional(readOnly = true)
    public Map<Long, RatingStats> statsForGames(Collection<Long> gameIds) {
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.aggregateByGameIds(gameIds).stream()
                .collect(Collectors.toMap(GameRatingRow::gameId, GameRatingRow::toStats));
    }

    // Carrega a review e garante que ela pertence a quem esta chamando.
    // Editar/apagar/ver historico sao operacoes so do autor.
    private Review findOwnedReview(String username, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review nao encontrada"));
        if (!review.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Essa review nao e sua");
        }
        return review;
    }
}
