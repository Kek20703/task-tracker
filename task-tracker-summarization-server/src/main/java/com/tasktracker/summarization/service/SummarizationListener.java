package com.tasktracker.summarization.service;

import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SummarizationListener {

    private static final Logger log = LoggerFactory.getLogger(SummarizationListener.class);

    private final SummarizationService summarizationService;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String responsesTopic;

    public SummarizationListener(SummarizationService summarizationService,
                                 KafkaTemplate<Object, Object> kafkaTemplate,
                                 @Value("${summarization.responses-topic}") String responsesTopic) {
        this.summarizationService = summarizationService;
        this.kafkaTemplate = kafkaTemplate;
        this.responsesTopic = responsesTopic;
    }

    @KafkaListener(topics = "${summarization.requests-topic}")
    public void onRequest(SummarizationRequest request) {
        if (request == null || request.requestId() == null) {
            log.warn("Ignoring summarization request without requestId");
            return;
        }
        SummarizationResponse response;
        try {
            response = summarizationService.summarize(request);
        } catch (RuntimeException e) {
            log.error("Summarization failed for request {}", request.requestId(), e);
            response = new SummarizationResponse(
                    request.requestId(), "Не удалось подготовить отчёт.", false);
        }
        try {
            kafkaTemplate.send(responsesTopic, request.requestId().toString(), response)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Failed to send summarization response for request {}",
                                    request.requestId(), error);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to send summarization response for request {}", request.requestId(), e);
        }
    }
}
