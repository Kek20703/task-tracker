package com.tasktracker.backend.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("tasks")
public class Task extends UuidIdentified {

    /** Владелец. Хранится как id, а не связь с User: задаче не нужно загружать пользователя. */
    @Column("user_id")
    private UUID userId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("status")
    private TaskStatus status;

    @Column("completed_at")
    private Instant completedAt;

    /** Заполняется аудитом Spring Data JDBC при вставке. */
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    protected Task() {
    }

    public Task(UUID userId, String title, String description) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = TaskStatus.TODO;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void describe(String description) {
        this.description = description;
    }

    /**
     * Меняет статус и поддерживает время выполнения: при переходе в DONE оно выставляется,
     * при возврате в TODO обнуляется. Повторная отметка уже сделанной задачи время не сдвигает.
     */
    public void changeStatus(TaskStatus newStatus, Instant now) {
        if (newStatus == status) {
            return;
        }
        status = newStatus;
        completedAt = newStatus == TaskStatus.DONE ? now : null;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
