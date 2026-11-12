package com.tasktracker.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    private static final UUID USER_ID = UUID.fromString("018f6d2e-1b7f-7cc1-9f31-3f6b8a2e4c5d");
    private static final Instant MORNING = Instant.parse("2026-11-11T09:00:00Z");
    private static final Instant EVENING = Instant.parse("2026-11-11T21:00:00Z");

    @Test
    void newTaskIsNotDone() {
        Task task = new Task(USER_ID, "Buy milk", "");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void markingDoneRecordsTime() {
        Task task = new Task(USER_ID, "Buy milk", "");

        task.changeStatus(TaskStatus.DONE, MORNING);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isEqualTo(MORNING);
    }

    @Test
    void markingDoneAgainKeepsOriginalTime() {
        Task task = new Task(USER_ID, "Buy milk", "");
        task.changeStatus(TaskStatus.DONE, MORNING);

        task.changeStatus(TaskStatus.DONE, EVENING);

        assertThat(task.getCompletedAt()).isEqualTo(MORNING);
    }

    @Test
    void reopeningClearsTimeAndCompletingAgainRecordsNewTime() {
        Task task = new Task(USER_ID, "Buy milk", "");
        task.changeStatus(TaskStatus.DONE, MORNING);

        task.changeStatus(TaskStatus.TODO, EVENING);
        assertThat(task.getCompletedAt()).isNull();

        task.changeStatus(TaskStatus.DONE, EVENING);
        assertThat(task.getCompletedAt()).isEqualTo(EVENING);
    }
}
