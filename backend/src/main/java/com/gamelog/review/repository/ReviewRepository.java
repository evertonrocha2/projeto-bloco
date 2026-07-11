package com.gamelog.review.repository;

import com.gamelog.review.domain.Review;
import com.gamelog.review.dto.GameRatingRow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;

// Alem do CRUD do JpaRepository, esse repositorio estende RevisionRepository:
// como Review e @Audited, o Spring Data + Envers nos dao findRevisions(id) de
// graca pra consultar o historico completo de uma review.
public interface ReviewRepository extends JpaRepository<Review, Long>, RevisionRepository<Review, Long, Long> {

    // Reviews de um jogo, das mais novas pras mais antigas.
    List<Review> findByGameIdOrderByCreatedAtDesc(Long gameId);

    // Reviews escritas por um usuario (usado na pagina de perfil).
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Usado pra impedir que o mesmo usuario avalie o mesmo jogo duas vezes.
    Optional<Review> findByUserIdAndGameId(Long userId, Long gameId);

    // Media e contagem de notas calculadas NO BANCO, agrupadas por jogo.
    // Antes a media era somada em memoria, jogo por jogo (um SELECT por card
    // do catalogo - o classico problema N+1). Agora e UMA consulta pra lista
    // inteira, e o banco so devolve dois numeros por jogo em vez de todas as
    // linhas de review.
    @Query("""
            select new com.gamelog.review.dto.GameRatingRow(r.game.id, avg(r.rating), count(r))
            from Review r
            where r.game.id in :gameIds
            group by r.game.id
            """)
    List<GameRatingRow> aggregateByGameIds(@Param("gameIds") Collection<Long> gameIds);
}
