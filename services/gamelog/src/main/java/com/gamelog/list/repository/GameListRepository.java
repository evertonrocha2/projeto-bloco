package com.gamelog.list.repository;

import com.gamelog.list.domain.GameList;
import com.gamelog.list.domain.ListVisibility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameListRepository extends JpaRepository<GameList, Long> {

    // Todas as listas de alguem - inclusive as privadas. So o proprio dono ve
    // isso.
    List<GameList> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    // O que um visitante ve no perfil de outra pessoa.
    List<GameList> findByOwnerIdAndVisibilityOrderByCreatedAtDesc(
            Long ownerId, ListVisibility visibility);

    // Descoberta por tag.
    //
    // O filtro de visibilidade esta DENTRO da consulta, e nao numa filtragem
    // depois: e a tela em que vazar uma lista privada seria mais facil e mais
    // silencioso, porque quem consulta por tag nunca e o dono.
    @Query("""
            select distinct l
            from GameList l
            join l.tags tag
            where tag = :tag
              and l.visibility = com.gamelog.list.domain.ListVisibility.PUBLIC
            order by l.createdAt desc
            """)
    List<GameList> findPublicByTag(@Param("tag") String tag);

    // Quantos jogos cada lista tem, pra um conjunto de listas de uma vez.
    //
    // O perfil mostra "12 jogos" em cada cartao. Carregar os itens de todas as
    // listas so pra contar traria a colecao inteira da pessoa pra memoria, e o
    // numero e a unica coisa que a tela usa.
    //
    // Listas vazias nao aparecem no resultado - nao ha linha pra agrupar. Quem
    // chama trata a ausencia como zero.
    @Query("""
            select i.list.id, count(i)
            from GameListItem i
            where i.list.id in :listIds
            group by i.list.id
            """)
    List<Object[]> countItemsByListIds(@Param("listIds") List<Long> listIds);
}
