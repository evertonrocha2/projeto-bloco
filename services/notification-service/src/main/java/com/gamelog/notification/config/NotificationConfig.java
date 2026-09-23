package com.gamelog.notification.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
