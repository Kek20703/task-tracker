package com.tasktracker.scheduler.dto;

/** Сообщение для `EMAIL_SENDING_TASKS`; формат совпадает с backend и email-sender. */
public record EmailMessage(String to, String subject, String text) {
}
