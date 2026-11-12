package com.tasktracker.summarization.dto;

import java.time.Instant;

/** Задача в запросе на отчёт. Формат совпадает с DTO scheduler. */
public record SummarizationTask(String title, String status, Instant completedAt, Instant createdAt) {
}
