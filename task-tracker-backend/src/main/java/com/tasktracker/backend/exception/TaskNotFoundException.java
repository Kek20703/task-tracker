package com.tasktracker.backend.exception;

/**
 * Задачи нет — или она принадлежит другому пользователю. Для клиента это неразличимо:
 * так нельзя узнать, существует ли чужая задача с данным id.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException() {
        super("Task not found");
    }
}
