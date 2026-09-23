package com.gamelog.recommendation.messaging;

import com.gamelog.recommendation.projection.ProcessedEventRepository;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Esquece os eventIds com mais de uma semana. Ver ProcessedEventRepository.
@Component
public class ProcessedEventCleanup {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventCleanup.class);

    private final ProcessedEventRepository repository;
    private final Clock clock;

    public ProcessedEventCleanup(ProcessedEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.processed-events.cleanup-cron:0 30 4 * * *}")
    public void cleanUp() {
        int removed = repository.deleteProcessedBefore(clock.instant().minus(Duration.ofDays(7)));
        if (removed > 0) {
            log.info("{} registros antigos de eventos processados removidos", removed);
        }
    }
}
