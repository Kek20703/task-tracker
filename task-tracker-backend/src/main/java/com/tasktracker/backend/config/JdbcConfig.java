package com.tasktracker.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Включает аудит Spring Data JDBC: {@code @CreatedDate} заполняется при вставке.
 */
@Configuration
@EnableJdbcAuditing
public class JdbcConfig {
}
