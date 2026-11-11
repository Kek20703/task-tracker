package com.tasktracker.emailsender.dto;

/**
 * Письмо из топика {@code EMAIL_SENDING_TASKS}. Это контракт сообщения между сервисами:
 * ровно такой же DTO пишет backend в топик. Поля должны совпадать по именам и типам.
 */
public record EmailMessage(String to, String subject, String text) {
}
