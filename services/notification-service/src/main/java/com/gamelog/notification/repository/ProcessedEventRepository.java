package com.gamelog.notification.repository;

import com.gamelog.notification.domain.ProcessedEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    // O monolito para de reenviar um evento assim que recebe a confirmacao do
    // broker; uma semana de memoria cobre com folga qualquer reentrega real.
    @Transactional
    @Modifying
    @Query("delete from ProcessedEvent p where p.processedAt < :cutoff")
    int deleteProcessedBefore(@Param("cutoff") Instant cutoff);
}
