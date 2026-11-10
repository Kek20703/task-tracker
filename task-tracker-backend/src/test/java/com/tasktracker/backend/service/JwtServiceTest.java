package com.tasktracker.backend.service;

import com.tasktracker.backend.TestTokens;
import com.tasktracker.backend.dto.CurrentUser;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 48 символов: хватает и для HS256 (32 байта), и для HS384 (48 байт).
    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmnopqrst";
    private static final String OTHER_SECRET = "another-secret-0123456789-abcdefghijklmnopqrstuv";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

    @Test
    void issuedTokenIsParsedBack() {
        String token = jwtService.issue(42L, "my@email.com");

        assertThat(jwtService.parse(token)).contains(new CurrentUser(42L, "my@email.com"));
    }

    @Test
    void acceptsHandMadeTokenOfTheSameShape() {
        // Убеждаемся, что заготовка TestTokens.valid() сама по себе годная:
        // иначе тесты ниже отклоняли бы токены не по той причине, которую проверяют.
        assertThat(jwtService.parse(TestTokens.sign(TestTokens.valid(42L, "my@email.com"), SECRET)))
                .contains(new CurrentUser(42L, "my@email.com"));
    }

    @Test
    void rejectsExpiredToken() {
        assertThat(jwtService.parse(TestTokens.sign(TestTokens.expired(42L, "my@email.com"), SECRET))).isEmpty();
    }

    @Test
    void rejectsTokenWithoutExpiration() {
        String eternal = TestTokens.sign(Jwts.builder().subject("42").claim("email", "my@email.com"), SECRET);

        assertThat(jwtService.parse(eternal)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        assertThat(jwtService.parse(TestTokens.sign(TestTokens.valid(42L, "my@email.com"), OTHER_SECRET))).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherAlgorithm() {
        assertThat(jwtService.parse(TestTokens.signWithHs384(TestTokens.valid(42L, "my@email.com"), SECRET)))
                .isEmpty();
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
    void rejectsTokenWithoutSubject() {
        String token = TestTokens.sign(Jwts.builder().claim("email", "my@email.com").expiration(inOneHour()), SECRET);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithNonNumericSubject() {
        String token = TestTokens.sign(
                Jwts.builder().subject("admin").claim("email", "my@email.com").expiration(inOneHour()), SECRET);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithoutEmail() {
        String token = TestTokens.sign(Jwts.builder().subject("42").expiration(inOneHour()), SECRET);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithNonStringEmail() {
        String token = TestTokens.sign(Jwts.builder().subject("42").claim("email", 123).expiration(inOneHour()), SECRET);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.parse("not-a-token")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
    }

    @Test
    void refusesToStartWithShortOrEmptySecret() {
        assertThatThrownBy(() -> new JwtService("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes")
                .hasMessageNotContaining("too-short");

        // Пустое значение — то, что получится, если скопировать .env.example и не задать секрет.
        assertThatThrownBy(() -> new JwtService("", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("got 0");
    }

    @Test
    void refusesToStartWithNonPositiveTtl() {
        assertThatThrownBy(() -> new JwtService(SECRET, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> new JwtService(SECRET, Duration.ofMinutes(-5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be positive");
    }

    private static Date inOneHour() {
        return Date.from(Instant.now().plusSeconds(3600));
    }
}
