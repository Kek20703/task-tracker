package com.tasktracker.backend.dto;

import com.tasktracker.backend.validation.NoNulCharacters;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Новая задача. Описание необязательно: в интерфейсе задача создаётся по одному заголовку. */
public record TaskCreateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = TaskLimits.TITLE_MAX, message = "Title is too long")
        @NoNulCharacters(message = "Title must not contain NUL characters")
        String title,

        @Size(max = TaskLimits.DESCRIPTION_MAX, message = "Description is too long")
        @NoNulCharacters(message = "Description must not contain NUL characters")
        String description
) {
}
