package com.tasktracker.backend;

import com.tasktracker.backend.entity.User;
import com.tasktracker.backend.repository.UserRepository;
import com.tasktracker.backend.service.WelcomeEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationIT {

    @MockitoBean
    private WelcomeEmailService welcomeEmailService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registersUserFromJson() throws Exception {
        register("my@email.com", "secret-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.email").value("my@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail("my@email.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("secret-password");
        assertThat(passwordEncoder.matches("secret-password", saved.getPassword())).isTrue();
    }

    @Test
    void registersUserFromForm() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "form@email.com")
                        .param("password", "secret-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("form@email.com"));

        assertThat(userRepository.existsByEmail("form@email.com")).isTrue();
    }

    @Test
    void storesEmailInLowerCase() throws Exception {
        register("Mixed.Case@Email.COM", "secret-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mixed.case@email.com"));

        assertThat(userRepository.findAll()).extracting(User::getEmail).containsExactly("mixed.case@email.com");
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        register("my@email.com", "secret-password").andExpect(status().isOk());

        register("MY@Email.com", "another-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email is already taken"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void databaseItselfRejectsDuplicateEmailIgnoringCase() {
        userRepository.save(new User("my@email.com", "hash"));

        assertThatThrownBy(() -> userRepository.save(new User("MY@EMAIL.COM", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createdAtIsAvailableRightAfterSave() {
        User saved = userRepository.save(new User("my@email.com", "hash"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        register("not-an-email", "secret-password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is not valid"));
    }

    @Test
    void rejectsShortPassword() throws Exception {
        register("my@email.com", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long"));
    }

    @Test
    void acceptsPasswordOfExactly72Bytes() throws Exception {
        register("my@email.com", "a".repeat(72)).andExpect(status().isOk());
    }

    @Test
    void rejectsPasswordLongerThan72Bytes() throws Exception {
        register("my@email.com", "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is too long"));
    }

    @Test
    void rejectsCyrillicPasswordThatExceeds72BytesButNot72Characters() throws Exception {
        // 40 символов, но 80 байт в UTF-8: BCrypt такое не принимает.
        register("my@email.com", "п".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is too long"));
    }

    @Test
    void acceptsCyrillicPasswordWithinLimit() throws Exception {
        register("my@email.com", "пароль-из-кириллицы").andExpect(status().isOk());
    }

    @Test
    void rejectsEmptyJsonObject() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required; Password is required"));
    }

    @Test
    void rejectsInvalidForm() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "form@email.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=my@email.com"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported Media Type"));
    }

    @Test
    void onlyPostOnUserIsOpen() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }
}
