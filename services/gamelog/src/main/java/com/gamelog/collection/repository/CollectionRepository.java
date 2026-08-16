package com.gamelog.collection.repository;

import com.gamelog.collection.domain.CollectionEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;

// CollectionEntry e @Audited, entao alem do CRUD esse repositorio tambem
// estende RevisionRepository: findRevisions(id) devolve a linha do tempo de um
// item da colecao (cada mudanca de horas/status que ja aconteceu).
public interface CollectionRepository extends JpaRepository<CollectionEntry, Long>, RevisionRepository<CollectionEntry, Long, Long> {

    // Itens da colecao de um usuario, dos mais recentes pros mais antigos.
    List<CollectionEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Usado pra saber se o jogo ja esta na colecao (ai a gente atualiza em vez de duplicar).
    Optional<CollectionEntry> findByUserIdAndGameId(Long userId, Long gameId);

    // Ids dos jogos que o usuario tem na colecao. O microsservico de recomendacoes
    // usa essa lista pra descartar candidatos: nao faz sentido recomendar um jogo
    // que a pessoa ja marcou como dela.
    //
    // Devolve so os ids de proposito. Carregar CollectionEntry inteiro traria
    // horas e status - que aqui nao interessam - e ainda cobraria uma consulta
    // extra por item pra resolver o Game LAZY.
    @Query("select ce.game.id from CollectionEntry ce where ce.user.username = :username")
    List<Long> findOwnedGameIdsByUsername(@Param("username") String username);
}
