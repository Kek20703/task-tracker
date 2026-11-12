package com.tasktracker.backend.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Первичный ключ назначается приложением перед вставкой и является UUIDv7. */
class UuidIdentifiedTest {

    @Test
    void assignsVersion7IdBeforeInsert() {
        User user = new User("user@example.com", "hash");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getId().version()).as("UUIDv7, а не случайный v4").isEqualTo(7);
    }

    @Test
    void idsAreMonotonicInCreationOrder() {
        User first = new User("first@email.com", "hash");
        User second = new User("second@email.com", "hash");

        // Сортировка задач по id остаётся порядком создания.
        assertThat(first.getId()).isLessThan(second.getId());
    }
}
