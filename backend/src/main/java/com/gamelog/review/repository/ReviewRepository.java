package com.gamelog.review.repository;

import com.gamelog.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Reviews de um jogo, das mais novas pras mais antigas.
    List<Review> findByGameIdOrderByCreatedAtDesc(Long gameId);

    // Reviews escritas por um usuario (usado na pagina de perfil).
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Usado pra impedir que o mesmo usuario avalie o mesmo jogo duas vezes.
    Optional<Review> findByUserIdAndGameId(Long userId, Long gameId);
}
