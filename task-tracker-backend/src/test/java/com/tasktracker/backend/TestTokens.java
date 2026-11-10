package com.tasktracker.backend;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Собирает токены вручную, в обход JwtService — чтобы проверять, как сервис относится к токенам,
 * которые сам он никогда бы не выпустил: просроченным, без срока действия, с чужой подписью.
 */
public final class TestTokens {

    private TestTokens() {
    }

    /** Заготовка корректного токена: sub, email и срок действия через час. Подпись ставит {@link #sign}. */
    public static JwtBuilder valid(Long userId, String email) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .expiration(Date.from(Instant.now().plusSeconds(3600)));
    }

    public static JwtBuilder expired(Long userId, String email) {
        return valid(userId, email).expiration(Date.from(Instant.now().minusSeconds(300)));
    }

    public static String sign(JwtBuilder builder, String secret) {
        return builder.signWith(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"),
                Jwts.SIG.HS256).compact();
    }

    /** Та же подпись тем же секретом, но алгоритмом HS384 вместо принятого в приложении HS256. */
    public static String signWithHs384(JwtBuilder builder, String secret) {
        return builder.signWith(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA384"),
                Jwts.SIG.HS384).compact();
    }
}
