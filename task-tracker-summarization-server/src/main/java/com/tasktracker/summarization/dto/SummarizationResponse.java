package com.tasktracker.summarization.dto;

import java.util.UUID;

/** Ответ на запрос отчёта; {@code requestId} связывает ответ с запросом. */
public record SummarizationResponse(UUID requestId, String report, boolean usedLlm) {
}
