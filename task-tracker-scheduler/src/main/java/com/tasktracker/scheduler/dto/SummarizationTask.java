package com.tasktracker.scheduler.dto;

import java.time.Instant;

/** Задача в запросе на отчёт. Формат совпадает с summarization-сервисом. */
public record SummarizationTask(String title, String status, Instant completedAt, Instant createdAt) {
}
