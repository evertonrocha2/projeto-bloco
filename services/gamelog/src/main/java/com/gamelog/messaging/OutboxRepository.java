package com.gamelog.messaging;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // O lote que o relay tenta entregar, na ordem em que os fatos foram gravados.
    // Limitado a 100 pra um acumulo grande (broker fora do ar por uma hora) nao
    // virar uma unica transacao gigante quando ele voltar.
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();

    long countByPublishedAtIsNull();

    // Faxina: evento entregue ha mais de alguns dias nao serve pra mais nada, e a
    // tabela cresceria pra sempre.
    @Transactional
    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt is not null and e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
