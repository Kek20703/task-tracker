package com.tasktracker.backend.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.util.UUID;

/**
 * Первичный ключ — UUIDv7. Назначается приложением перед вставкой; в базе на колонке есть
 * {@code default uuidv7()} как страховка для записей вне приложения.
 * UUIDv7 монотонны по времени: индекс остаётся дружелюбным, а количество записей наружу не раскрывают.
 */
@MappedSuperclass
public abstract class UuidIdentified {

    @Id
    private UUID id;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    public UUID getId() {
        return id;
    }
}
