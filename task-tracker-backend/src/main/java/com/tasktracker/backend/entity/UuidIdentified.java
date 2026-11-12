package com.tasktracker.backend.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.UUID;

/**
 * Первичный ключ — UUIDv7. Назначается приложением при создании сущности; в базе на колонке есть
 * {@code default uuidv7()} как страховка для записей вне приложения.
 * UUIDv7 монотонны по времени: индекс остаётся дружелюбным, а количество записей наружу не раскрывают.
 *
 * <p>{@link Version} нужен Spring Data JDBC, чтобы отличить новую сущность от загруженной:
 * у новой версия {@code null}, после вставки и загрузки — число.
 */
public abstract class UuidIdentified {

    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Version
    private Long version;

    public UUID getId() {
        return id;
    }
}
