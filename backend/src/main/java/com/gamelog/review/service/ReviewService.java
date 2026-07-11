package com.gamelog.review.service;

import com.gamelog.catalog.domain.Game;
import com.gamelog.catalog.repository.GameRepository;
import com.gamelog.identity.domain.User;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.domain.Review;
import com.gamelog.review.dto.CreateReviewRequest;
import com.gamelog.review.dto.RatingStats;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Concentra tudo que envolve reviews: criar uma nova, listar as de um jogo,
// listar as de um usuario e calcular a media de notas. Como uma review liga
// usuario e jogo, esse service conversa com os tres repositorios.
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         GameRepository gameRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
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
        return ReviewResponse.from(review);
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

    // Calcula media e contagem das notas de um jogo. Como o catalogo e pequeno,
    // somar em memoria aqui e suficiente e simples de entender.
    @Transactional(readOnly = true)
    public RatingStats statsForGame(Long gameId) {
        List<Review> reviews = reviewRepository.findByGameIdOrderByCreatedAtDesc(gameId);
        if (reviews.isEmpty()) {
            return RatingStats.empty();
        }
        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        return new RatingStats(average, reviews.size());
    }
}
