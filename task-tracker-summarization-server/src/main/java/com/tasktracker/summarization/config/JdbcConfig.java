package com.tasktracker.summarization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "truncatedDateTimeProvider")
public class JdbcConfig {

    @Bean
    DateTimeProvider truncatedDateTimeProvider() {
        return () -> Optional.<TemporalAccessor>of(
                Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}
