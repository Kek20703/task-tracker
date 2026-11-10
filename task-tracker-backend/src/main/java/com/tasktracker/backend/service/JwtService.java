package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Выпускает и проверяет JWT access token (подпись HMAC-SHA256). */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.ttl}") Duration ttl) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long, got " + secretBytes.length);
        }
        this.key = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.ttl = ttl;
    }

    public String issue(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(EMAIL_CLAIM, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Возвращает пользователя из токена, если подпись верна и срок действия не истёк.
     * Любой другой токен — чужой, просроченный, повреждённый — даёт пустой результат.
     */
    public Optional<CurrentUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String email = claims.get(EMAIL_CLAIM, String.class);
            if (claims.getSubject() == null || email == null) {
                return Optional.empty();
            }
            return Optional.of(new CurrentUser(Long.valueOf(claims.getSubject()), email));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
