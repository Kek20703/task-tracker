package com.tasktracker.backend.dto;

import com.tasktracker.backend.validation.NoNulCharacters;
import com.tasktracker.backend.validation.NullOrNotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Частичное обновление задачи: меняются только переданные поля, отсутствующее или null поле
 * остаётся как было. Так интерфейс может сохранять каждое изменение отдельно — заголовок,
 * описание или чекбокс — без кнопки «Сохранить». Описание очищается пустой строкой.
 */
public record TaskUpdateRequest(

        @NullOrNotBlank(message = "Title must not be blank")
        @Size(max = TaskLimits.TITLE_MAX, message = "Title is too long")
        @NoNulCharacters(message = "Title must not contain NUL characters")
        String title,

        @Size(max = TaskLimits.DESCRIPTION_MAX, message = "Description is too long")
        @NoNulCharacters(message = "Description must not contain NUL characters")
        String description,

        @Pattern(regexp = "TODO|DONE", message = "Status must be TODO or DONE")
        String status
) {
}
