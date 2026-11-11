package com.tasktracker.backend.repository;

import com.tasktracker.backend.entity.Task;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByUserIdOrderByIdAsc(UUID userId);

    /**
     * Задача для изменения или удаления: ищется сразу с владельцем (чужая для запроса не существует)
     * и блокируется до конца транзакции ({@code SELECT ... FOR UPDATE}).
     * Одновременные автосохранения одной задачи выполняются по очереди, и каждое видит результат
     * предыдущего, а не затирает его. Если задачу успели удалить, второй запрос её просто не найдёт.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Task> findForUpdateByIdAndUserId(UUID id, UUID userId);
}
