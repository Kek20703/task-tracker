package com.tasktracker.scheduler.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SummarizationRequest(
        UUID requestId,
        UUID userId,
        Instant from,
        Instant to,
        List<SummarizationTask> tasks
) {
}
