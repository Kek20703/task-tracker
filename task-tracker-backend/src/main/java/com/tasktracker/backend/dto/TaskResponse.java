package com.tasktracker.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Задача в ответах API. {@code status} — TODO или DONE; {@code completedAt} заполнено только у
 * сделанной задачи. Даты — строки ISO-8601 в UTC.
 */
public record TaskResponse(
        UUID id,
        String title,
        String description,
        String status,
        Instant completedAt,
        Instant createdAt
) {
}
