package com.gamelog.collection.repository;

import com.gamelog.collection.domain.CollectionEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<CollectionEntry, Long> {

    // Itens da colecao de um usuario, dos mais recentes pros mais antigos.
    List<CollectionEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Usado pra saber se o jogo ja esta na colecao (ai a gente atualiza em vez de duplicar).
    Optional<CollectionEntry> findByUserIdAndGameId(Long userId, Long gameId);
}
