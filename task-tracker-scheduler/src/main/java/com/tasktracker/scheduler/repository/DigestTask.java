package com.tasktracker.scheduler.repository;

import java.time.Instant;
import java.util.UUID;

/** Проекция задачи для дневного отчёта. */
public record DigestTask(UUID id, String title, String status, Instant completedAt, Instant createdAt) {
}
