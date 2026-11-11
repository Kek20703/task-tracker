package com.tasktracker.backend.dto;

/** Единый формат ответа с ошибкой: JSON-объект с полем message. */
public record ErrorDto(String message) {
}
