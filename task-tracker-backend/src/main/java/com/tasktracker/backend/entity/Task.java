package com.tasktracker.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task extends UuidIdentified {

    /** Владелец. Хранится как id, а не связь с User: задаче не нужно загружать пользователя. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Заполняется базой при вставке и сразу перечитывается. */
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
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
