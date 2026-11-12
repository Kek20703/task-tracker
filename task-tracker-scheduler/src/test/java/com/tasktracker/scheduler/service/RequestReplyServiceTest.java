package com.tasktracker.scheduler.service;

import com.tasktracker.scheduler.dto.SummarizationRequest;
import com.tasktracker.scheduler.dto.SummarizationResponse;
import com.tasktracker.scheduler.exception.SummarizationUnavailableException;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestReplyServiceTest {

    private final KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void returnsResponseWithMatchingRequestId() throws Exception {
        RequestReplyService service = new RequestReplyService(
                kafkaTemplate, "requests", Duration.ofSeconds(2));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.<SendResult<Object, Object>>completedFuture(null));
        SummarizationRequest request = request();
        SummarizationResponse response = new SummarizationResponse(request.requestId(), "Готово", true);

        Future<SummarizationResponse> future = pool.submit(() -> service.ask(request));
        Thread.sleep(100);
        service.onResponse(response);

        assertThat(future.get(2, TimeUnit.SECONDS).report()).isEqualTo("Готово");
    }

    @Test
    void timeoutBecomesSummarizationUnavailable() {
        RequestReplyService service = new RequestReplyService(
                kafkaTemplate, "requests", Duration.ofMillis(50));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.<SendResult<Object, Object>>completedFuture(null));

        assertThatThrownBy(() -> service.ask(request()))
                .isInstanceOf(SummarizationUnavailableException.class);
    }

    @Test
    void synchronousKafkaFailureBecomesSummarizationUnavailable() {
        RequestReplyService service = new RequestReplyService(
                kafkaTemplate, "requests", Duration.ofSeconds(2));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new KafkaException("down"));

        assertThatThrownBy(() -> service.ask(request()))
                .isInstanceOf(SummarizationUnavailableException.class);
    }

    @Test
    void unknownResponseIsIgnored() {
        RequestReplyService service = new RequestReplyService(
                kafkaTemplate, "requests", Duration.ofSeconds(2));

        service.onResponse(new SummarizationResponse(UUID.randomUUID(), "late", false));

        // Не падает и не трогает KafkaTemplate.
    }

    private static SummarizationRequest request() {
        Instant now = Instant.parse("2026-11-12T10:00:00Z");
        return new SummarizationRequest(UUID.randomUUID(), UUID.randomUUID(), now, now, List.of());
    }
}
