package com.tasktracker.scheduler.dto;

import java.util.UUID;

public record SummarizationResponse(UUID requestId, String report, boolean usedLlm) {
}
