package com.tasktracker.backend.dto;

/** Ограничения длины полей задачи. Совпадают с размерами колонок в миграции 002. */
public final class TaskLimits {

    public static final int TITLE_MAX = 255;
    public static final int DESCRIPTION_MAX = 10_000;

    private TaskLimits() {
    }
}
