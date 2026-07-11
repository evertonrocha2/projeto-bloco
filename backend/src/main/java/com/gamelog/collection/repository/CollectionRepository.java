package com.gamelog.collection.repository;

import com.gamelog.collection.domain.CollectionEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

// CollectionEntry e @Audited, entao alem do CRUD esse repositorio tambem
// estende RevisionRepository: findRevisions(id) devolve a linha do tempo de um
// item da colecao (cada mudanca de horas/status que ja aconteceu).
public interface CollectionRepository extends JpaRepository<CollectionEntry, Long>, RevisionRepository<CollectionEntry, Long, Long> {

    // Itens da colecao de um usuario, dos mais recentes pros mais antigos.
    List<CollectionEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Usado pra saber se o jogo ja esta na colecao (ai a gente atualiza em vez de duplicar).
    Optional<CollectionEntry> findByUserIdAndGameId(Long userId, Long gameId);
}
