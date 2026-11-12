package com.tasktracker.scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class SchedulerConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId schedulerZone(@Value("${scheduler.zone:UTC}") String zone) {
        return ZoneId.of(zone);
    }
}
