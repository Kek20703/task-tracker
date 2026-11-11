package com.tasktracker.backend;

import com.tasktracker.backend.dto.TaskCreateRequest;
import com.tasktracker.backend.dto.TaskResponse;
import com.tasktracker.backend.dto.TaskUpdateRequest;
import com.tasktracker.backend.entity.Task;
import com.tasktracker.backend.entity.User;
import com.tasktracker.backend.exception.TaskNotFoundException;
import com.tasktracker.backend.repository.TaskRepository;
import com.tasktracker.backend.repository.UserRepository;
import com.tasktracker.backend.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Одновременные запросы к одной задаче. Первый «запрос» — транзакция, которая блокирует задачу так же,
 * как это делает сервис, меняет её и держит блокировку. Второй — настоящий вызов сервиса, который
 * в это время должен ждать, а не работать со старым снимком строки.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TaskConcurrencyIT {

    private static final Duration HOLD = Duration.ofMillis(700);

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    private Long userId;
    private Long taskId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.saveAndFlush(new User("alice@email.com", "hash")).getId();
        taskId = taskService.create(userId, new TaskCreateRequest("Original", null)).id();
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void concurrentAutosavesDoNotOverwriteEachOther() throws Exception {
        // Пользователь меняет заголовок и сразу щёлкает чекбокс: два PATCH почти одновременно.
        Future<?> first = holdLockedTask(task -> task.rename("Renamed by first request"));

        Future<TaskResponse> second = pool.submit(
                () -> taskService.update(userId, taskId, new TaskUpdateRequest(null, null, "DONE")));

        assertThat(waitsForLock(second)).as("второй запрос ждёт, пока первый держит задачу").isTrue();
        first.get(5, TimeUnit.SECONDS);
        TaskResponse response = second.get(5, TimeUnit.SECONDS);

        assertThat(response.title()).isEqualTo("Renamed by first request");
        assertThat(response.status()).isEqualTo("DONE");
        Task saved = taskRepository.findById(taskId).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Renamed by first request");
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    void updateOfTaskDeletedConcurrentlyIsNotFound() throws Exception {
        Future<?> first = holdLockedTask(task -> taskRepository.delete(task));

        Future<TaskResponse> second = pool.submit(
                () -> taskService.update(userId, taskId, new TaskUpdateRequest("too late", null, null)));

        first.get(5, TimeUnit.SECONDS);
        assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void secondConcurrentDeleteIsNotFound() throws Exception {
        Future<?> first = holdLockedTask(task -> taskRepository.delete(task));

        Future<?> second = pool.submit(() -> taskService.delete(userId, taskId));

        first.get(5, TimeUnit.SECONDS);
        assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TaskNotFoundException.class);
        assertThat(taskRepository.existsById(taskId)).isFalse();
    }

    /** Запускает транзакцию, которая блокирует задачу, применяет к ней действие и держит блокировку {@link #HOLD}. */
    private Future<?> holdLockedTask(Consumer<Task> action) throws InterruptedException {
        CountDownLatch locked = new CountDownLatch(1);
        Future<?> future = pool.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            Task task = taskRepository.findForUpdateByIdAndUserId(taskId, userId).orElseThrow();
            action.accept(task);
            taskRepository.flush();
            locked.countDown();
            sleep(HOLD);
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).as("первая транзакция взяла блокировку").isTrue();
        return future;
    }

    private static boolean waitsForLock(Future<?> future) {
        sleep(HOLD.dividedBy(3));
        return !future.isDone();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
