package com.gamelog.catalog.repository;

import com.gamelog.catalog.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

// CRUD pronto pros jogos. Nao precisamos de consultas customizadas aqui:
// o findAll e o findById que vem do JpaRepository ja resolvem o catalogo.
public interface GameRepository extends JpaRepository<Game, Long> {
}
