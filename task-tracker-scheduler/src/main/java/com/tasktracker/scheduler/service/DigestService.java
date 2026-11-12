package com.tasktracker.scheduler.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.tasktracker.scheduler.repository.DigestTask;
import com.tasktracker.scheduler.dto.EmailMessage;
import com.tasktracker.scheduler.dto.SummarizationRequest;
import com.tasktracker.scheduler.dto.SummarizationTask;
import com.tasktracker.scheduler.repository.UserDigest;
import com.tasktracker.scheduler.exception.SummarizationUnavailableException;
import com.tasktracker.scheduler.repository.DigestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DigestService {

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);
    private static final String SUBJECT = "Итоги дня в Task Tracker";

    private final DigestRepository repository;
    private final RequestReplyService requestReplyService;
    private final EmailService emailService;
    private final Clock clock;
    private final ZoneId zone;

    public DigestService(DigestRepository repository, RequestReplyService requestReplyService,
                         EmailService emailService, Clock clock, ZoneId zone) {
        this.repository = repository;
        this.requestReplyService = requestReplyService;
        this.emailService = emailService;
        this.clock = clock;
        this.zone = zone;
    }

    /** Запускает рассылку за предыдущий календарный день; возвращает число писем. */
    public int runForPreviousDay() {
        Period period = previousDay();
        List<UserDigest> users;
        try {
            users = repository.findUsersWithTasks(period.from(), period.to());
        } catch (RuntimeException e) {
            log.error("Failed to select users for daily digest", e);
            return 0;
        }

        int sent = 0;
        for (UserDigest user : users) {
            try {
                String report = report(user, period);
                emailService.send(new EmailMessage(
                        user.email(), SUBJECT, report + "\n\nПериод: "
                        + period.from() + " — " + period.to()));
                sent++;
            } catch (RuntimeException e) {
                log.warn("Failed to prepare daily digest for user {}", user.userId(), e);
            }
        }
        return sent;
    }

    public Period previousDay() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        LocalDate today = now.toLocalDate();
        Instant to = today.atStartOfDay(zone).toInstant();
        Instant from = today.minusDays(1).atStartOfDay(zone).toInstant();
        return new Period(from, to);
    }

    private String report(UserDigest user, Period period) {
        SummarizationRequest request = new SummarizationRequest(
                UuidCreator.getTimeOrderedEpoch(), user.userId(), period.from(), period.to(),
                user.tasks().stream()
                        .map(task -> new SummarizationTask(
                                task.title(), task.status(), task.completedAt(), task.createdAt()))
                        .toList());
        try {
            return requestReplyService.ask(request).report();
        } catch (SummarizationUnavailableException e) {
            log.warn("Summarization unavailable for user {}, using template", user.userId(), e);
            return template(user.tasks(), period);
        }
    }

    static String template(List<DigestTask> tasks, Period period) {
        long done = tasks.stream().filter(task -> "DONE".equals(task.status())).count();
        long left = tasks.size() - done;
        return "Отчёт за период с " + period.from() + " по " + period.to() + ". "
                + "Всего задач: " + tasks.size() + ", выполнено: " + done + ", осталось: " + left + ".";
    }

    public record Period(Instant from, Instant to) {
    }
}
