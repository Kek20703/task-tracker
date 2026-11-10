package com.tasktracker.backend;

import com.tasktracker.backend.entity.User;
import com.tasktracker.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationIT {

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
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("my@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail("my@email.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("secret-password");
        assertThat(passwordEncoder.matches("secret-password", saved.getPassword())).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
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
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        register("my@email.com", "secret-password").andExpect(status().isOk());

        register("MY@Email.com", "another-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email is already taken"));

        assertThat(userRepository.count()).isEqualTo(1);
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
                .andExpect(jsonPath("$.message").value("Password must be 8 to 72 characters long"));
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
                .andExpect(jsonPath("$.message").isNotEmpty());
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
