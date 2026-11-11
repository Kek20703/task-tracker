package com.tasktracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Новая задача. Описание необязательно: в интерфейсе задача создаётся по одному заголовку. */
public record TaskCreateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = TaskLimits.TITLE_MAX, message = "Title is too long")
        String title,

        @Size(max = TaskLimits.DESCRIPTION_MAX, message = "Description is too long")
        String description
) {
}
