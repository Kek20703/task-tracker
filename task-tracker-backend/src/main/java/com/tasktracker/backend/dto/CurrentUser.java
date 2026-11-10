package com.tasktracker.backend.dto;

/** Пользователь, от имени которого выполняется запрос. Берётся из проверенного JWT. */
public record CurrentUser(Long id, String email) {
}
