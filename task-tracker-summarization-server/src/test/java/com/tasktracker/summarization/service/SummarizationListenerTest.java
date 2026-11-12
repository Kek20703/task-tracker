package com.tasktracker.summarization.service;

import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummarizationListenerTest {

    private final SummarizationService service = mock(SummarizationService.class);
    private final KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final SummarizationListener listener =
            new SummarizationListener(service, kafkaTemplate, "SUMMARIZATION_RESPONSES");

    @Test
    void sendsResponseWithRequestIdAsKey() {
        SummarizationRequest request = request();
        SummarizationResponse response = new SummarizationResponse(request.requestId(), "Отчёт", true);
        when(service.summarize(request)).thenReturn(response);
        when(kafkaTemplate.send(eq("SUMMARIZATION_RESPONSES"), eq(request.requestId().toString()), any()))
                .thenReturn(CompletableFuture.<SendResult<Object, Object>>completedFuture(null));

        listener.onRequest(request);

        verify(kafkaTemplate).send(eq("SUMMARIZATION_RESPONSES"),
                eq(request.requestId().toString()), eq(response));
    }

    @Test
    void serviceFailureStillSendsFallback() {
        SummarizationRequest request = request();
        when(service.summarize(request)).thenThrow(new IllegalStateException("boom"));
        when(kafkaTemplate.send(eq("SUMMARIZATION_RESPONSES"), eq(request.requestId().toString()), any()))
                .thenReturn(CompletableFuture.<SendResult<Object, Object>>completedFuture(null));

        listener.onRequest(request);

        verify(kafkaTemplate).send(eq("SUMMARIZATION_RESPONSES"),
                eq(request.requestId().toString()), eq(new SummarizationResponse(
                        request.requestId(), "Не удалось подготовить отчёт.", false)));
    }

    @Test
    void responseHasRequestId() {
        SummarizationRequest request = request();
        SummarizationResponse response = new SummarizationResponse(request.requestId(), "Отчёт", false);

        assertThat(response.requestId()).isEqualTo(request.requestId());
    }

    @Test
    void ignoresRequestWithoutRequestId() {
        listener.onRequest(null);

        org.mockito.Mockito.verifyNoInteractions(service, kafkaTemplate);
    }

    @Test
    void synchronousSendFailureDoesNotEscape() {
        SummarizationRequest request = request();
        when(service.summarize(request))
                .thenReturn(new SummarizationResponse(request.requestId(), "Отчёт", false));
        when(kafkaTemplate.send(eq("SUMMARIZATION_RESPONSES"), eq(request.requestId().toString()), any()))
                .thenThrow(new org.apache.kafka.common.KafkaException("down"));

        org.assertj.core.api.Assertions.assertThatCode(() -> listener.onRequest(request))
                .doesNotThrowAnyException();
    }

    private static SummarizationRequest request() {
        return new SummarizationRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                java.time.Instant.parse("2026-11-12T00:00:00Z"),
                java.time.Instant.parse("2026-11-13T00:00:00Z"),
                List.of());
    }
}
