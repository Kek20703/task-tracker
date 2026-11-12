package com.tasktracker.summarization.service;

import com.tasktracker.summarization.dto.LlmResult;
import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationResponse;
import com.tasktracker.summarization.entity.SummarizationRecord;
import com.tasktracker.summarization.repository.SummarizationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final LlmService llmService;
    private final SummarizationRecordRepository repository;

    public SummarizationService(LlmService llmService, SummarizationRecordRepository repository) {
        this.llmService = llmService;
        this.repository = repository;
    }

    public SummarizationResponse summarize(SummarizationRequest request) {
        LlmResult result = llmService.summarize(request);
        saveHistory(request, result);
        return new SummarizationResponse(request.requestId(), result.report(), result.usedLlm());
    }

    private void saveHistory(SummarizationRequest request, LlmResult result) {
        try {
            int taskCount = request.tasks() == null ? 0 : request.tasks().size();
            repository.save(new SummarizationRecord(
                    request.requestId(), request.userId(), request.from(), request.to(),
                    taskCount, result.report(), result.usedLlm()));
        } catch (RuntimeException e) {
            log.warn("Failed to save summarization history for request {}", request.requestId(), e);
        }
    }
}
