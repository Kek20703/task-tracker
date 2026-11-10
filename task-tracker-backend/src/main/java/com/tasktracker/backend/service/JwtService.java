package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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
    private static final String ALGORITHM = "HS256";
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.ttl}") Duration ttl) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes long, got "
                            + secretBytes.length + ". Generate one with: openssl rand -hex 32");
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("jwt.ttl (JWT_TTL) must be positive, got " + ttl);
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
     * Любой другой токен — чужой, просроченный, повреждённый, без срока действия — даёт пустой результат.
     */
    public Optional<CurrentUser> parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();
            String email = claims.get(EMAIL_CLAIM, String.class);
            // Принимаются только токены того вида, который выпускает issue():
            // тот же алгоритм, обязательные срок действия, id и email.
            if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())
                    || claims.getExpiration() == null
                    || claims.getSubject() == null
                    || email == null) {
                return Optional.empty();
            }
            return Optional.of(new CurrentUser(Long.valueOf(claims.getSubject()), email));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
