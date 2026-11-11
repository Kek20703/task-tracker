package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.TaskCreateRequest;
import com.tasktracker.backend.entity.Task;
import com.tasktracker.backend.exception.AccountNotFoundException;
import com.tasktracker.backend.repository.TaskRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final UserService userService = mock(UserService.class);
    private final TaskService taskService = new TaskService(taskRepository, userService);

    private final TaskCreateRequest request = new TaskCreateRequest("Buy milk", null);

    @Test
    void userDeletedBetweenCheckAndInsertMeansNoAccount() {
        when(taskRepository.saveAndFlush(any(Task.class))).thenThrow(new DataIntegrityViolationException("fk",
                new ConstraintViolationException("fk", new SQLException("fk", "23503"), "fk_tasks_user")));

        assertThatThrownBy(() -> taskService.create(1L, request)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void otherDataErrorsAreNotMaskedAsMissingAccount() {
        // Например, значение, которое Postgres не принял. Живой пользователь не должен получить 401.
        when(taskRepository.saveAndFlush(any(Task.class))).thenThrow(new DataIntegrityViolationException("data",
                new DataException("bad data", new SQLException("bad data", "22021"))));

        assertThatThrownBy(() -> taskService.create(1L, request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void otherConstraintViolationsAreNotMaskedAsMissingAccount() {
        when(taskRepository.saveAndFlush(any(Task.class))).thenThrow(new DataIntegrityViolationException("check",
                new ConstraintViolationException("check", new SQLException("check", "23514"), "ck_tasks_status")));

        assertThatThrownBy(() -> taskService.create(1L, request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
