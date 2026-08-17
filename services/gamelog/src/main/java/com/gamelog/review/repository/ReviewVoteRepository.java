package com.gamelog.review.repository;

import com.gamelog.review.domain.ReviewVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    // Usado pra decidir entre criar, trocar de lado ou desfazer.
    Optional<ReviewVote> findByUserIdAndReviewId(Long userId, Long reviewId);

    // Quantos positivos e quantos negativos cada avaliacao tem, pra um conjunto
    // de avaliacoes de uma vez.
    //
    // A pagina de um jogo lista N avaliacoes. Contar os dois lados avaliacao por
    // avaliacao seriam 2N consultas pra desenhar uma tela - e o numero cresce com
    // a popularidade do jogo, ou seja, a pagina fica mais lenta exatamente onde
    // mais gente entra. Aqui o banco agrupa e devolve tudo numa ida so.
    //
    // Cada linha vem como (reviewId, tipo, total).
    @Query("""
            select rv.review.id, rv.type, count(rv)
            from ReviewVote rv
            where rv.review.id in :reviewIds
            group by rv.review.id, rv.type
            """)
    List<Object[]> countByTypeForReviews(@Param("reviewIds") List<Long> reviewIds);

    // Em quais destas avaliacoes EU votei, e de que lado. Tambem em lote, pelo
    // mesmo motivo das contagens: e o que marca o polegar aceso na tela.
    List<ReviewVote> findByUserIdAndReviewIdIn(Long userId, List<Long> reviewIds);
}
