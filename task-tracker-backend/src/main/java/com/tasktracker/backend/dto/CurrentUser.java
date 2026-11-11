package com.tasktracker.backend.dto;

/** Пользователь, от имени которого выполняется запрос. Берётся из проверенного JWT. */
import java.util.UUID;

public record CurrentUser(UUID id, String email) {
}
