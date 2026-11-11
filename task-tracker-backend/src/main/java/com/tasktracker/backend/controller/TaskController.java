package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.CurrentUser;
import com.tasktracker.backend.dto.TaskCreateRequest;
import com.tasktracker.backend.dto.TaskResponse;
import com.tasktracker.backend.dto.TaskUpdateRequest;
import com.tasktracker.backend.service.TaskService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Задачи текущего пользователя")
    @GetMapping
    public List<TaskResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
        return taskService.list(CurrentUsers.requireId(currentUser));
    }

    @Operation(summary = "Создать задачу", description = "Обязателен только заголовок. Новая задача создаётся в статусе TODO.")
    @ApiResponse(responseCode = "201", description = "Задача создана; адрес — в заголовке Location",
            headers = @Header(name = "Location", description = "Адрес новой задачи", schema = @Schema(type = "string")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                               @Valid @RequestBody TaskCreateRequest request) {
        TaskResponse task = taskService.create(CurrentUsers.requireId(currentUser), request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(task.id()))
                .body(task);
    }

    @Operation(summary = "Изменить задачу", description = "Меняются только переданные поля; null и отсутствующее поле не трогаются. "
            + "status: TODO или DONE — при DONE сервер ставит completedAt, при TODO обнуляет. Описание очищается пустой строкой.")
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskResponse update(@AuthenticationPrincipal CurrentUser currentUser,
                               @PathVariable UUID id,
                               @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(CurrentUsers.requireId(currentUser), id, request);
    }

    @Operation(summary = "Удалить задачу")
    @ApiResponse(responseCode = "204", description = "Задача удалена")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        taskService.delete(CurrentUsers.requireId(currentUser), id);
        return ResponseEntity.noContent().build();
    }
}
