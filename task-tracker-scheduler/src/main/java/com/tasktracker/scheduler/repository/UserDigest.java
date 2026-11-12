package com.tasktracker.scheduler.repository;

import java.util.List;
import java.util.UUID;

/** Пользователь и его задачи, отобранные для дневного отчёта. */
public record UserDigest(UUID userId, String email, List<DigestTask> tasks) {
}
