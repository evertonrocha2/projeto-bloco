package com.gamelog.recommendation.repository;

import com.gamelog.recommendation.domain.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorio das recomendacoes geradas - parte da camada de persistencia
// dedicada do microsservico.
//
// Mesmo padrao do monolito (Spring Data, consultas derivadas do nome do metodo),
// mas apontando pra OUTRO banco. Os dois servicos usam a mesma tecnologia e
// nenhuma tabela em comum.
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // Como a tela le: da maior pontuacao pra menor. A ordenacao sai do banco.
    List<Recommendation> findByUsernameOrderByScoreDesc(String username);

    // Recalcular = apagar o lote antigo e gravar o novo.
    //
    // Por que @Modifying com JPQL em vez do "deleteByUsername" derivado que o
    // Spring Data geraria de graca: o metodo derivado CARREGA as entidades e as
    // marca como removidas, deixando o DELETE pendente no contexto de persistencia.
    // Na hora do flush o Hibernate ordena as operacoes por tipo - INSERT antes de
    // DELETE - entao a nova recomendacao de um jogo era inserida enquanto a linha
    // antiga do mesmo jogo ainda existia, e a constraint (username, game_id)
    // estourava. O recalculo simplesmente nao funcionava.
    //
    // Este delete e em massa: roda o SQL na hora, na ordem em que foi chamado.
    // flushAutomatically garante que nada fique pendente antes dele, e
    // clearAutomatically limpa o contexto depois - senao entidades ja apagadas no
    // banco continuariam vivas em memoria.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Recommendation r where r.username = :username")
    void deleteByUsername(@Param("username") String username);

    // Usado pra decidir entre servir o lote existente e gerar um na hora, na
    // primeira vez que alguem abre a tela.
    boolean existsByUsername(String username);

    // Tira uma recomendacao especifica do lote - o que acontece quando o usuario
    // curte ou descarta um card. Delete em massa, pelo mesmo motivo do de cima:
    // roda o SQL na ordem em que foi chamado, sem ficar pendente no contexto.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Recommendation r where r.username = :username and r.gameId = :gameId")
    void deleteByUsernameAndGameId(@Param("username") String username,
                                   @Param("gameId") Long gameId);
}
