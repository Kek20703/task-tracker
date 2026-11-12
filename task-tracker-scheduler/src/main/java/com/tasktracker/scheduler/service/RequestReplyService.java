package com.tasktracker.scheduler.service;

import com.tasktracker.scheduler.dto.SummarizationRequest;
import com.tasktracker.scheduler.dto.SummarizationResponse;
import com.tasktracker.scheduler.exception.SummarizationUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Kafka RPC: отправляет запрос и ждёт ответ с тем же requestId. */
@Service
public class RequestReplyService {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String requestsTopic;
    private final Duration timeout;
    private final ConcurrentHashMap<UUID, CompletableFuture<SummarizationResponse>> pending =
            new ConcurrentHashMap<>();

    public RequestReplyService(KafkaTemplate<Object, Object> kafkaTemplate,
                               @Value("${summarization.requests-topic}") String requestsTopic,
                               @Value("${scheduler.request-timeout:30s}") Duration timeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.requestsTopic = requestsTopic;
        this.timeout = timeout;
    }

    public SummarizationResponse ask(SummarizationRequest request) {
        UUID requestId = request.requestId();
        CompletableFuture<SummarizationResponse> future = new CompletableFuture<>();
        pending.put(requestId, future);
        try {
            kafkaTemplate.send(requestsTopic, requestId.toString(), request);
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SummarizationUnavailableException("Interrupted while waiting for report", e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new SummarizationUnavailableException("Summarization service did not answer", e);
        } finally {
            pending.remove(requestId);
        }
    }

    @KafkaListener(topics = "${summarization.responses-topic}")
    public void onResponse(SummarizationResponse response) {
        if (response == null || response.requestId() == null) {
            return;
        }
        CompletableFuture<SummarizationResponse> future = pending.remove(response.requestId());
        if (future != null) {
            future.complete(response);
        }
    }
}
