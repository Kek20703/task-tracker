package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.TaskCreateRequest;
import com.tasktracker.backend.dto.TaskResponse;
import com.tasktracker.backend.dto.TaskUpdateRequest;
import com.tasktracker.backend.entity.Task;
import com.tasktracker.backend.entity.TaskStatus;
import com.tasktracker.backend.exception.AccountNotFoundException;
import com.tasktracker.backend.exception.TaskNotFoundException;
import com.tasktracker.backend.repository.TaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Задачи текущего пользователя. Каждая операция начинается с проверки, что пользователь из токена
 * ещё существует: токен удалённого пользователя действителен до истечения срока, но работать не должен.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long userId) {
        userService.requireExisting(userId);
        return taskRepository.findAllByUserIdOrderByIdAsc(userId).stream().map(TaskService::toResponse).toList();
    }

    public TaskResponse create(Long userId, TaskCreateRequest request) {
        userService.requireExisting(userId);
        String description = request.description() == null ? "" : request.description();
        Task task = new Task(userId, request.title().strip(), description);
        try {
            return toResponse(taskRepository.saveAndFlush(task));
        } catch (DataIntegrityViolationException e) {
            // Пользователя удалили между проверкой и вставкой — внешний ключ не пустил задачу.
            throw new AccountNotFoundException();
        }
    }

    public TaskResponse update(Long userId, Long taskId, TaskUpdateRequest request) {
        userService.requireExisting(userId);
        Task task = findOwned(userId, taskId);
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

    public void delete(Long userId, Long taskId) {
        userService.requireExisting(userId);
        taskRepository.delete(findOwned(userId, taskId));
    }

    private Task findOwned(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId).orElseThrow(TaskNotFoundException::new);
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
