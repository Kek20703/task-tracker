package com.tasktracker.summarization.service;

import com.tasktracker.summarization.dto.LlmResult;
import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationResponse;
import com.tasktracker.summarization.dto.SummarizationTask;
import com.tasktracker.summarization.entity.SummarizationRecord;
import com.tasktracker.summarization.repository.SummarizationRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummarizationServiceTest {

    private final LlmService llmService = mock(LlmService.class);
    private final SummarizationRecordRepository repository = mock(SummarizationRecordRepository.class);
    private final SummarizationService service = new SummarizationService(llmService, repository);

    @Test
    void returnsLlmReportAndStoresHistory() {
        SummarizationRequest request = request();
        when(llmService.summarize(request)).thenReturn(new LlmResult("Готовый отчёт", true));

        SummarizationResponse response = service.summarize(request);

        assertThat(response.requestId()).isEqualTo(request.requestId());
        assertThat(response.report()).isEqualTo("Готовый отчёт");
        assertThat(response.usedLlm()).isTrue();

        ArgumentCaptor<SummarizationRecord> saved = ArgumentCaptor.forClass(SummarizationRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRequestId()).isEqualTo(request.requestId());
        assertThat(saved.getValue().getTaskCount()).isEqualTo(2);
        assertThat(saved.getValue().getReport()).isEqualTo("Готовый отчёт");
        assertThat(saved.getValue().isUsedLlm()).isTrue();
    }

    @Test
    void databaseFailureDoesNotLoseResponse() {
        SummarizationRequest request = request();
        when(llmService.summarize(request)).thenReturn(new LlmResult("Отчёт", false));
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("db is down"));

        assertThatCode(() -> service.summarize(request)).doesNotThrowAnyException();
        assertThat(service.summarize(request).report()).isEqualTo("Отчёт");
    }

    private static SummarizationRequest request() {
        Instant now = Instant.parse("2026-11-12T10:00:00Z");
        return new SummarizationRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                now.minusSeconds(86400), now,
                List.of(
                        new SummarizationTask("Сделано", "DONE", now, now),
                        new SummarizationTask("Осталось", "TODO", null, now)));
    }
}
