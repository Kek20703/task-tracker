package com.tasktracker.summarization.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Запрос отчёта через Kafka RPC. */
public record SummarizationRequest(
        UUID requestId,
        UUID userId,
        Instant from,
        Instant to,
        List<SummarizationTask> tasks
) {
}
