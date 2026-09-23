package com.gamelog.recommendation.projection;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRatingViewRepository extends JpaRepository<UserRatingView, Long> {

    Optional<UserRatingView> findByUsernameAndGameId(String username, Long gameId);

    List<UserRatingView> findByUsernameAndDeletedFalse(String username);

    // Media da comunidade por jogo, calculada AQUI a partir das notas copiadas.
    //
    // No TP3 esse numero vinha pronto do monolito. Como a projecao recebe toda
    // review criada, editada e apagada, ela tem os mesmos insumos e chega na mesma
    // media - sem perguntar a ninguem. Linhas [gameId, media].
    @Query("""
            select r.gameId, avg(r.rating)
            from UserRatingView r
            where r.deleted = false
            group by r.gameId
            """)
    List<Object[]> averageRatingByGame();
}
