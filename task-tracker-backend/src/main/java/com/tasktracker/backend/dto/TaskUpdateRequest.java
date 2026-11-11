package com.tasktracker.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Частичное обновление задачи: меняются только переданные поля, отсутствующее или null поле
 * остаётся как было. Так интерфейс может сохранять каждое изменение отдельно — заголовок,
 * описание или чекбокс — без кнопки «Сохранить». Описание очищается пустой строкой.
 */
public record TaskUpdateRequest(

        @Pattern(regexp = "(?s).*\\S.*", message = "Title must not be blank")
        @Size(max = TaskLimits.TITLE_MAX, message = "Title is too long")
        String title,

        @Size(max = TaskLimits.DESCRIPTION_MAX, message = "Description is too long")
        String description,

        @Pattern(regexp = "TODO|DONE", message = "Status must be TODO or DONE")
        String status
) {
}
