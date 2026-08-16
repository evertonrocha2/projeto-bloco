package com.gamelog.catalog.repository;

import com.gamelog.catalog.domain.Game;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// CRUD pronto pros jogos (save, findById, findAll...) via JpaRepository.
// As consultas extras sao "derived queries": o Spring Data gera o SQL a partir
// do nome do metodo, sem a gente escrever uma linha de implementacao.
public interface GameRepository extends JpaRepository<Game, Long> {

    // Busca paginada por titulo (case-insensitive). Devolver Page em vez de
    // List evita carregar o catalogo inteiro na memoria: o banco so traz a
    // pagina pedida (LIMIT/OFFSET) e ainda informa o total pra montar a paginacao.
    Page<Game> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // O import da RAWG usa isso pra nao inserir o mesmo jogo duas vezes
    // (aproveita o indice idx_games_external_id).
    Optional<Game> findByExternalId(Long externalId);
}
