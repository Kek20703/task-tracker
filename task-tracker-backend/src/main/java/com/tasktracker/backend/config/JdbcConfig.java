package com.tasktracker.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * Включает аудит Spring Data JDBC: {@code @CreatedDate} заполняется при вставке.
 * Время обрезается до микросекунд — так же, как его хранит Postgres.
 */
@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "truncatedDateTimeProvider")
public class JdbcConfig {

    @Bean
    DateTimeProvider truncatedDateTimeProvider() {
        return () -> Optional.<TemporalAccessor>of(
                Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}
