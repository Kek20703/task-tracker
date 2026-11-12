package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.TaskCreateRequest;
import com.tasktracker.backend.entity.Task;
import com.tasktracker.backend.exception.AccountNotFoundException;
import com.tasktracker.backend.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.UUID;

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
        when(taskRepository.save(any(Task.class))).thenThrow(
                new DataIntegrityViolationException("fk", new SQLException("fk_tasks_user")));

        assertThatThrownBy(() -> taskService.create(UUID.randomUUID(), request))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void otherDataErrorsAreNotMaskedAsMissingAccount() {
        // Например, битые данные, которые Postgres не принял. Такая ошибка не должна выглядеть как 401.
        when(taskRepository.save(any(Task.class))).thenThrow(
                new DataIntegrityViolationException("data", new SQLException("bad data")));

        assertThatThrownBy(() -> taskService.create(UUID.randomUUID(), request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void otherConstraintViolationsAreNotMaskedAsMissingAccount() {
        when(taskRepository.save(any(Task.class))).thenThrow(
                new DataIntegrityViolationException("check", new SQLException("ck_tasks_status")));

        assertThatThrownBy(() -> taskService.create(UUID.randomUUID(), request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
