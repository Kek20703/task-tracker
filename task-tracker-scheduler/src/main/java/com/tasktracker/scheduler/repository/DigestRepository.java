package com.tasktracker.scheduler.repository;

import com.tasktracker.scheduler.repository.DigestTask;
import com.tasktracker.scheduler.repository.UserDigest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Читает задачи из схемы backend. Только SELECT, без миграций и записи. */
@Repository
public class DigestRepository {

    private final JdbcTemplate jdbcTemplate;

    public DigestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserDigest> findUsersWithTasks(Instant from, Instant to) {
        List<UserRow> users = jdbcTemplate.query("""
                        SELECT u.id, u.email
                        FROM users u
                        WHERE EXISTS (
                            SELECT 1 FROM tasks t
                            WHERE t.user_id = u.id
                              AND ((t.completed_at >= ? AND t.completed_at < ?) OR t.status = 'TODO')
                        )
                        ORDER BY u.email
                        """,
                (rs, rowNum) -> new UserRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("email")),
                Timestamp.from(from), Timestamp.from(to));

        return users.stream()
                .map(user -> new UserDigest(user.id(), user.email(), findTasks(user.id(), from, to)))
                .toList();
    }

    private List<DigestTask> findTasks(UUID userId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                        SELECT id, title, status, completed_at, created_at
                        FROM tasks
                        WHERE user_id = ?
                          AND ((completed_at >= ? AND completed_at < ?) OR status = 'TODO')
                        ORDER BY created_at, id
                        """,
                (rs, rowNum) -> {
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    return new DigestTask(
                            rs.getObject("id", UUID.class),
                            rs.getString("title"),
                            rs.getString("status"),
                            completedAt == null ? null : completedAt.toInstant(),
                            rs.getTimestamp("created_at").toInstant());
                },
                userId, Timestamp.from(from), Timestamp.from(to));
    }

    private record UserRow(UUID id, String email) {
    }
}
