package com.tasktracker.backend.repository;

import com.tasktracker.backend.entity.Task;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends CrudRepository<Task, UUID> {

    List<Task> findAllByUserIdOrderByIdAsc(UUID userId);

    /**
     * Задача для изменения или удаления: ищется сразу с владельцем (чужая для запроса не существует)
     * и блокируется до конца транзакции ({@code SELECT ... FOR UPDATE}).
     * Spring Data JDBC не поддерживает JPA-ный {@code @Lock}, поэтому блокировка задана в запросе.
     */
    @Query("SELECT * FROM tasks WHERE id = :id AND user_id = :userId FOR UPDATE")
    Optional<Task> findForUpdateByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
