package com.gamelog.notification.messaging;

import com.gamelog.notification.repository.ProcessedEventRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventCleanup {

    private final ProcessedEventRepository repository;
    private final Clock clock;

    public ProcessedEventCleanup(ProcessedEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.processed-events.cleanup-cron:0 45 4 * * *}")
    public void cleanUp() {
        repository.deleteProcessedBefore(clock.instant().minus(Duration.ofDays(7)));
    }
}
