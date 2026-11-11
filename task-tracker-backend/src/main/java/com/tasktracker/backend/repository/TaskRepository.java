package com.tasktracker.backend.repository;

import com.tasktracker.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByUserIdOrderByIdAsc(Long userId);

    /** Задача ищется сразу с владельцем: чужая задача для запроса просто не существует. */
    Optional<Task> findByIdAndUserId(Long id, Long userId);
}
