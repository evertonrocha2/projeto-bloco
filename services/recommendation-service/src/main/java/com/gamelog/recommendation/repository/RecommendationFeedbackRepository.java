package com.gamelog.recommendation.repository;

import com.gamelog.recommendation.domain.RecommendationFeedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio do feedback - o dado que so existe neste servico.
public interface RecommendationFeedbackRepository
        extends JpaRepository<RecommendationFeedback, Long> {

    // O algoritmo precisa de todos os vereditos de uma vez: LIKED reforca generos,
    // DISMISSED exclui candidatos. Uma consulta, nao uma por jogo.
    List<RecommendationFeedback> findByUsername(String username);

    // Usado ao registrar feedback: se ja existe veredito pra esse jogo, atualiza em
    // vez de inserir outro.
    Optional<RecommendationFeedback> findByUsernameAndGameId(String username, Long gameId);
}
