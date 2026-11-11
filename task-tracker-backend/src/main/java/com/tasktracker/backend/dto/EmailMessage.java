package com.tasktracker.backend.dto;

/**
 * Сообщение для топика {@code EMAIL_SENDING_TASKS}. Формат совпадает с DTO email-sender:
 * поля {@code to}, {@code subject}, {@code text}. Тип задаётся настройкой потребителя,
 * заголовки типа в сообщение не добавляются.
 */
public record EmailMessage(String to, String subject, String text) {
}
