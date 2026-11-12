package com.tasktracker.scheduler.repository;

import com.tasktracker.scheduler.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DigestRepositoryIT {

    private static final Instant FROM = Instant.parse("2026-11-11T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-12T00:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DigestRepository repository;

    private UUID alice;
    private UUID bob;
    private UUID carol;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists tasks");
        jdbcTemplate.execute("drop table if exists users");
        jdbcTemplate.execute("""
                create table users (
                    id uuid primary key,
                    email varchar(254) not null
                )
                """);
        jdbcTemplate.execute("""
                create table tasks (
                    id uuid primary key,
                    user_id uuid not null references users(id),
                    title varchar(255) not null,
                    description varchar(10000) not null default '',
                    status varchar(16) not null,
                    completed_at timestamptz,
                    created_at timestamptz not null default now()
                )
                """);

        alice = user("alice@example.com");
        bob = user("bob@example.com");
        carol = user("carol@example.com");

        task(alice, "Alice done yesterday", "DONE", Instant.parse("2026-11-11T10:00:00Z"));
        task(alice, "Alice still open", "TODO", null);
        task(bob, "Bob done long ago", "DONE", Instant.parse("2026-11-01T10:00:00Z"));
        task(carol, "Carol still open", "TODO", null);
    }

    @Test
    void selectsUsersWithCompletedTasksInPeriodOrOpenTasks() {
        List<UserDigest> users = repository.findUsersWithTasks(FROM, TO);

        assertThat(users).extracting(UserDigest::email)
                .containsExactly("alice@example.com", "carol@example.com");
        assertThat(users.get(0).tasks()).extracting(DigestTask::title)
                .containsExactlyInAnyOrder("Alice done yesterday", "Alice still open");
        assertThat(users.get(1).tasks()).extracting(DigestTask::title)
                .containsExactly("Carol still open");
    }

    private UUID user(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("insert into users(id, email) values (?, ?)", id, email);
        return id;
    }

    private void task(UUID userId, String title, String status, Instant completedAt) {
        jdbcTemplate.update("""
                        insert into tasks(id, user_id, title, status, completed_at)
                        values (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), userId, title, status,
                completedAt == null ? null : java.sql.Timestamp.from(completedAt));
    }
}
