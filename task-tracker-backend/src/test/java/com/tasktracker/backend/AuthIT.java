package com.tasktracker.backend;

import com.tasktracker.backend.repository.UserRepository;
import com.tasktracker.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIT {

    private static final String EMAIL = "my@email.com";
    private static final String PASSWORD = "secret-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // --- Регистрация выдаёт токен ---

    @Test
    void registrationReturnsTokenThatWorksImmediately() throws Exception {
        String authorization = tokenFrom(register(EMAIL, PASSWORD).andExpect(status().isOk()));

        assertThat(authorization).startsWith("Bearer ");
        mockMvc.perform(get("/user").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void failedRegistrationReturnsNoToken() throws Exception {
        register(EMAIL, PASSWORD).andExpect(status().isOk());

        register(EMAIL, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION));
    }

    // --- Вход ---

    @Test
    void loginWithJsonReturnsUserAndToken() throws Exception {
        register(EMAIL, PASSWORD);

        String authorization = tokenFrom(login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist()));

        mockMvc.perform(get("/user").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void loginWithFormReturnsToken() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void loginIgnoresEmailCase() throws Exception {
        register(EMAIL, PASSWORD);

        login("MY@Email.COM", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
        register(EMAIL, PASSWORD);

        login(EMAIL, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        login("nobody@email.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginValidatesRequest() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required; Password is required"));
    }

    @Test
    void loginWithTooLongPasswordIsRejectedWithoutServerError() throws Exception {
        register(EMAIL, PASSWORD);

        login(EMAIL, "п".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is too long"));
    }

    // --- Доступ по токену ---

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void garbageTokenIsUnauthorized() throws Exception {
        expectUnauthorized("Bearer not-a-token");
        expectUnauthorized("Bearer ");
        expectUnauthorized("not-even-bearer");
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        Long userId = registerAndGetId();
        String expired = new JwtService(TestcontainersConfiguration.TEST_JWT_SECRET, Duration.ofMinutes(-5))
                .issue(userId, EMAIL);

        expectUnauthorized("Bearer " + expired);
    }

    @Test
    void tokenSignedWithAnotherSecretIsUnauthorized() throws Exception {
        Long userId = registerAndGetId();
        String foreign = new JwtService("another-secret-0123456789-abcdefghijklmn", Duration.ofHours(1))
                .issue(userId, EMAIL);

        expectUnauthorized("Bearer " + foreign);
    }

    @Test
    void tokenOfDeletedUserIsUnauthorized() throws Exception {
        String authorization = tokenFrom(register(EMAIL, PASSWORD));
        userRepository.deleteAll();

        expectUnauthorized(authorization);
    }

    @Test
    void basicAuthWithRealCredentialsIsNotAccepted() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(get("/user").with(request -> {
                    request.addHeader(HttpHeaders.AUTHORIZATION, "Basic "
                            + java.util.Base64.getEncoder().encodeToString((EMAIL + ":" + PASSWORD).getBytes()));
                    return request;
                }))
                .andExpect(status().isUnauthorized());
    }

    // --- Формат ошибок для авторизованного пользователя ---

    @Test
    void unsupportedMethodReturns405InCommonFormat() throws Exception {
        String authorization = tokenFrom(register(EMAIL, PASSWORD));

        mockMvc.perform(delete("/user").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method Not Allowed"));
    }

    @Test
    void unknownPathReturns404InCommonFormat() throws Exception {
        String authorization = tokenFrom(register(EMAIL, PASSWORD));

        mockMvc.perform(get("/no-such-path").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not Found"));
    }

    private void expectUnauthorized(String authorization) throws Exception {
        mockMvc.perform(get("/user").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    private Long registerAndGetId() throws Exception {
        register(EMAIL, PASSWORD);
        return userRepository.findByEmail(EMAIL).orElseThrow().getId();
    }

    private static String tokenFrom(ResultActions actions) {
        MvcResult result = actions.andReturn();
        return result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    private static String credentials(String email, String password) {
        return "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password);
    }
}
