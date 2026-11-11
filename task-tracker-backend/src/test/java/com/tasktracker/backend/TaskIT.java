package com.tasktracker.backend;

import com.tasktracker.backend.entity.Task;
import com.tasktracker.backend.repository.TaskRepository;
import com.tasktracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TaskIT {

    private static final String ISO_INSTANT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private String alice;
    private String bob;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        alice = registerAndGetToken("alice@email.com");
        bob = registerAndGetToken("bob@email.com");
    }

    // --- Создание ---

    @Test
    void createsTaskWithTitleOnly() throws Exception {
        create(alice, "{\"title\": \"Buy milk\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/tasks/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.description").value(""))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern(ISO_INSTANT)));
    }

    @Test
    void createsTaskWithDescriptionAndTrimsTitle() throws Exception {
        create(alice, "{\"title\": \"  Buy milk  \", \"description\": \"2 liters\\nlow fat\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.description").value("2 liters\nlow fat"));
    }

    @Test
    void locationHeaderPointsToCreatedTask() throws Exception {
        ResultActions created = create(alice, "{\"title\": \"Buy milk\"}");
        long id = readJson(created).get("id").asLong();

        created.andExpect(header().string(HttpHeaders.LOCATION, endsWith("/tasks/" + id)));
    }

    @Test
    void rejectsInvalidNewTask() throws Exception {
        create(alice, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is required"));
        create(alice, "{\"title\": \"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is required"));
        create(alice, "{\"title\": \"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is too long"));
        create(alice, "{\"title\": \"ok\", \"description\": \"" + "a".repeat(10_001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Description is too long"));

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void acceptsFieldsOfMaximumLength() throws Exception {
        create(alice, "{\"title\": \"" + "a".repeat(255) + "\", \"description\": \"" + "б".repeat(10_000) + "\"}")
                .andExpect(status().isCreated());
    }

    // --- Список ---

    @Test
    void listsOnlyOwnTasksInCreationOrder() throws Exception {
        createAndRead(alice, "First");
        createAndRead(bob, "Bob's task");
        createAndRead(alice, "Second");

        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].title").value("Second"));
    }

    @Test
    void emptyListIsEmptyArray() throws Exception {
        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- Частичное обновление ---

    @Test
    void updatesOnlyGivenFields() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();
        update(alice, id, "{\"description\": \"2 liters\"}").andExpect(status().isOk());

        update(alice, id, "{\"title\": \"Buy kefir\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy kefir"))
                .andExpect(jsonPath("$.description").value("2 liters"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void emptyStringClearsDescriptionButNullKeepsIt() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();
        update(alice, id, "{\"description\": \"2 liters\"}");

        update(alice, id, "{\"description\": null}").andExpect(jsonPath("$.description").value("2 liters"));
        update(alice, id, "{\"description\": \"\"}").andExpect(jsonPath("$.description").value(""));
    }

    @Test
    void markingDoneSetsCompletionTimeAndReopeningClearsIt() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();

        String completedAt = readJson(update(alice, id, "{\"status\": \"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").value(matchesPattern(ISO_INSTANT))))
                .get("completedAt").asString();

        // Повторная отметка не сдвигает время выполнения.
        update(alice, id, "{\"status\": \"DONE\", \"title\": \"Bought milk\"}")
                .andExpect(jsonPath("$.completedAt").value(completedAt));

        // Ответ совпадает с тем, что лежит в базе: время не теряет точность при сохранении.
        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$[0].completedAt").value(completedAt));

        update(alice, id, "{\"status\": \"TODO\"}")
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
    }

    @Test
    void emptyPatchChangesNothing() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();

        update(alice, id, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy milk"));
    }

    @Test
    void rejectsInvalidUpdate() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();

        update(alice, id, "{\"title\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title must not be blank"));
        update(alice, id, "{\"title\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title must not be blank"));
        update(alice, id, "{\"status\": \"done\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status must be TODO or DONE"));
        update(alice, id, "{\"title\": \"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is too long"));

        Task unchanged = taskRepository.findById(id).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Buy milk");
    }

    // --- Удаление ---

    @Test
    void deletesTask() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();

        mockMvc.perform(delete("/tasks/" + id).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.existsById(id)).isFalse();
        mockMvc.perform(delete("/tasks/" + id).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    // --- Чужие и несуществующие задачи ---

    @Test
    void someoneElsesTaskLooksLikeMissingOne() throws Exception {
        long bobsTask = createAndRead(bob, "Bob's task").get("id").asLong();

        update(alice, bobsTask, "{\"title\": \"hacked\", \"status\": \"DONE\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
        mockMvc.perform(delete("/tasks/" + bobsTask).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
        update(alice, Long.MAX_VALUE, "{\"title\": \"x\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        Task untouched = taskRepository.findById(bobsTask).orElseThrow();
        assertThat(untouched.getTitle()).isEqualTo("Bob's task");
        assertThat(untouched.getCompletedAt()).isNull();
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        update(alice, "abc", "{\"title\": \"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    // --- Авторизация ---

    @Test
    void everyTaskEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/tasks/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/tasks/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedUserCannotWorkWithTasks() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();
        userRepository.delete(userRepository.findByEmail("alice@email.com").orElseThrow());

        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
        create(alice, "{\"title\": \"x\"}").andExpect(status().isUnauthorized());
        update(alice, id, "{\"title\": \"x\"}").andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/tasks/" + id).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isUnauthorized());
    }

    // --- Ввод, который не должен доходить до базы или давать 500 ---

    @Test
    void rejectsNulCharacters() throws Exception {
        create(alice, "{\"title\": \"a\\u0000b\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title must not contain NUL characters"));
        create(alice, "{\"title\": \"ok\", \"description\": \"a\\u0000b\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Description must not contain NUL characters"));

        long id = createAndRead(alice, "Buy milk").get("id").asLong();
        update(alice, id, "{\"title\": \"a\\u0000b\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title must not contain NUL characters"));
        update(alice, id, "{\"description\": \"a\\u0000b\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Description must not contain NUL characters"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"a\\u0000@email.com\", \"password\": \"secret-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must not contain NUL characters"));
    }

    @Test
    void unicodeWhitespaceTitleIsBlank() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();

        // Идеографический и em-пробел: String.strip() их убирает, значит и проверка должна их видеть.
        update(alice, id, "{\"title\": \"\\u3000\\u2003\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title must not be blank"));
        create(alice, "{\"title\": \"\\u3000\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is required"));

        assertThat(taskRepository.findById(id).orElseThrow().getTitle()).isEqualTo("Buy milk");
    }

    @Test
    void rejectsTooLongDescriptionOnUpdate() throws Exception {
        long id = createAndRead(alice, "Buy milk").get("id").asLong();

        update(alice, id, "{\"description\": \"" + "a".repeat(10_001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Description is too long"));
    }

    @Test
    void malformedAndUnsupportedBodiesUseCommonErrorFormat() throws Exception {
        create(alice, "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
        mockMvc.perform(post("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, alice)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Buy milk"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported Media Type"));
        mockMvc.perform(get("/tasks/1").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method Not Allowed"));
    }

    @Test
    void deletingUserDeletesTheirTasks() throws Exception {
        createAndRead(alice, "Buy milk");
        createAndRead(bob, "Bob's task");

        userRepository.delete(userRepository.findByEmail("alice@email.com").orElseThrow());

        assertThat(taskRepository.findAll()).extracting(Task::getTitle).containsExactly("Bob's task");
    }

    // --- Помощники ---

    private String registerAndGetToken(String email) throws Exception {
        return mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"secret-password\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.AUTHORIZATION);
    }

    private ResultActions create(String authorization, String json) throws Exception {
        return mockMvc.perform(post("/tasks")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private JsonNode createAndRead(String authorization, String title) throws Exception {
        return readJson(create(authorization, "{\"title\": \"%s\"}".formatted(title)).andExpect(status().isCreated()));
    }

    private ResultActions update(String authorization, Object id, String json) throws Exception {
        return mockMvc.perform(patch("/tasks/" + id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private JsonNode readJson(ResultActions actions) throws Exception {
        return jsonMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
