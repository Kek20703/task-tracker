package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.CurrentUser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijkl";
    private static final String OTHER_SECRET = "another-secret-0123456789-abcdefghijklmn";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

    @Test
    void issuedTokenIsParsedBack() {
        String token = jwtService.issue(42L, "my@email.com");

        assertThat(jwtService.parse(token)).contains(new CurrentUser(42L, "my@email.com"));
    }

    @Test
    void rejectsExpiredToken() {
        String expired = new JwtService(SECRET, Duration.ofMinutes(-5)).issue(42L, "my@email.com");

        assertThat(jwtService.parse(expired)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String foreign = new JwtService(OTHER_SECRET, Duration.ofHours(1)).issue(42L, "my@email.com");

        assertThat(jwtService.parse(foreign)).isEmpty();
    }

    @Test
    void rejectsTokenWithTamperedPayload() {
        String[] parts = jwtService.issue(42L, "my@email.com").split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), UTF_8).replace("\"42\"", "\"1\"");
        String tampered = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(UTF_8)) + "." + parts[2];

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void rejectsUnsignedToken() {
        // Заголовок {"alg":"none"} и то же содержимое без подписи.
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(UTF_8));
        String payload = jwtService.issue(42L, "my@email.com").split("\\.")[1];

        assertThat(jwtService.parse(header + "." + payload + ".")).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.parse("not-a-token")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
    }

    @Test
    void refusesToStartWithShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
