package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.TaskCreateRequest;
import com.tasktracker.backend.dto.TaskResponse;
import com.tasktracker.backend.dto.TaskUpdateRequest;
import com.tasktracker.backend.entity.Task;
import com.tasktracker.backend.entity.TaskStatus;
import com.tasktracker.backend.exception.AccountNotFoundException;
import com.tasktracker.backend.exception.TaskNotFoundException;
import com.tasktracker.backend.repository.TaskRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Задачи текущего пользователя. Каждая операция начинается с проверки, что пользователь из токена
 * ещё существует: токен удалённого пользователя действителен до истечения срока, но работать не должен.
 */
@Service
@Transactional
public class TaskService {

    /** Имя внешнего ключа tasks.user_id из миграции 002. */
    private static final String OWNER_FOREIGN_KEY = "fk_tasks_user";

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID userId) {
        userService.requireExisting(userId);
        return taskRepository.findAllByUserIdOrderByIdAsc(userId).stream().map(TaskService::toResponse).toList();
    }

    public TaskResponse create(UUID userId, TaskCreateRequest request) {
        userService.requireExisting(userId);
        String description = request.description() == null ? "" : request.description();
        Task task = new Task(userId, request.title().strip(), description);
        try {
            return toResponse(taskRepository.saveAndFlush(task));
        } catch (DataIntegrityViolationException e) {
            if (violatesOwnerForeignKey(e)) {
                // Пользователя удалили между проверкой и вставкой — внешний ключ не пустил задачу.
                throw new AccountNotFoundException();
            }
            throw e;
        }
    }

    public TaskResponse update(UUID userId, UUID taskId, TaskUpdateRequest request) {
        userService.requireExisting(userId);
        Task task = findOwnedForUpdate(userId, taskId);
        if (request.title() != null) {
            task.rename(request.title().strip());
        }
        if (request.description() != null) {
            task.describe(request.description());
        }
        if (request.status() != null) {
            task.changeStatus(TaskStatus.valueOf(request.status()), now());
        }
        return toResponse(taskRepository.saveAndFlush(task));
    }

    public void delete(UUID userId, UUID taskId) {
        userService.requireExisting(userId);
        taskRepository.delete(findOwnedForUpdate(userId, taskId));
    }

    /** Задача блокируется до конца транзакции — см. {@link TaskRepository#findForUpdateByIdAndUserId}. */
    private Task findOwnedForUpdate(UUID userId, UUID taskId) {
        return taskRepository.findForUpdateByIdAndUserId(taskId, userId).orElseThrow(TaskNotFoundException::new);
    }

    private static boolean violatesOwnerForeignKey(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && OWNER_FOREIGN_KEY.equalsIgnoreCase(violation.getConstraintName());
    }

    /** Postgres хранит время с точностью до микросекунд — обрезаем сразу, чтобы ответ совпадал с базой. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getCompletedAt(),
                task.getCreatedAt());
    }
}
