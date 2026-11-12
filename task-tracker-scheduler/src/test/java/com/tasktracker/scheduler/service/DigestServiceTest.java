package com.tasktracker.scheduler.service;

import com.tasktracker.scheduler.dto.EmailMessage;
import com.tasktracker.scheduler.dto.SummarizationResponse;
import com.tasktracker.scheduler.exception.SummarizationUnavailableException;
import com.tasktracker.scheduler.repository.DigestRepository;
import com.tasktracker.scheduler.repository.DigestTask;
import com.tasktracker.scheduler.repository.UserDigest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DigestServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T00:05:00Z");

    private final DigestRepository repository = mock(DigestRepository.class);
    private final RequestReplyService requestReplyService = mock(RequestReplyService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final DigestService service = new DigestService(
            repository, requestReplyService, emailService,
            Clock.fixed(NOW, ZoneOffset.UTC), ZoneId.of("UTC"));

    @Test
    void sendsLlmReportForSelectedUser() {
        UserDigest user = user();
        when(repository.findUsersWithTasks(any(), any())).thenReturn(List.of(user));
        when(requestReplyService.ask(any())).thenReturn(
                new SummarizationResponse(UUID.randomUUID(), "Отчёт от LLM", true));

        int sent = service.runForPreviousDay();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(message.capture());
        assertThat(message.getValue().to()).isEqualTo("user@example.com");
        assertThat(message.getValue().subject()).isEqualTo("Итоги дня в Task Tracker");
        assertThat(message.getValue().text()).contains("Отчёт от LLM");
    }

    @Test
    void usesTemplateWhenSummarizationIsUnavailable() {
        when(repository.findUsersWithTasks(any(), any())).thenReturn(List.of(user()));
        when(requestReplyService.ask(any()))
                .thenThrow(new SummarizationUnavailableException("no answer"));

        assertThat(service.runForPreviousDay()).isEqualTo(1);

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(message.capture());
        assertThat(message.getValue().text())
                .contains("Всего задач: 2, выполнено: 1, осталось: 1");
    }

    @Test
    void databaseFailureDoesNotCrashScheduler() {
        when(repository.findUsersWithTasks(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("db is down"));

        assertThat(service.runForPreviousDay()).isZero();
    }

    @Test
    void previousDayDependsOnConfiguredZone() {
        DigestService utc = service;
        DigestService moscow = new DigestService(
                repository, requestReplyService, emailService,
                Clock.fixed(NOW, ZoneOffset.UTC), ZoneId.of("Europe/Moscow"));

        assertThat(utc.previousDay().from()).isEqualTo(Instant.parse("2026-11-11T00:00:00Z"));
        assertThat(utc.previousDay().to()).isEqualTo(Instant.parse("2026-11-12T00:00:00Z"));
        assertThat(moscow.previousDay().from()).isEqualTo(Instant.parse("2026-11-10T21:00:00Z"));
        assertThat(moscow.previousDay().to()).isEqualTo(Instant.parse("2026-11-11T21:00:00Z"));
    }

    private static UserDigest user() {
        Instant now = Instant.parse("2026-11-11T10:00:00Z");
        return new UserDigest(UUID.randomUUID(), "user@example.com", List.of(
                new DigestTask(UUID.randomUUID(), "Сделано", "DONE", now, now.minusSeconds(3600)),
                new DigestTask(UUID.randomUUID(), "Осталось", "TODO", null, now.minusSeconds(1800))));
    }
}
