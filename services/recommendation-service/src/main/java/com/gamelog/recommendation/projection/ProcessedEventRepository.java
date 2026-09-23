package com.gamelog.recommendation.projection;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    // Uma duplicata chega, na pratica, segundos ou minutos depois da original. Uma
    // semana de memoria cobre com folga qualquer reentrega, e impede a tabela de
    // crescer pra sempre.
    @Transactional
    @Modifying
    @Query("delete from ProcessedEvent e where e.processedAt < :before")
    int deleteProcessedBefore(@Param("before") Instant before);
}
