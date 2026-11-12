package com.lms.identity;

import com.lms.shared.Times;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtService {
    private final SecretKey key;
    private final AuthProperties auth;

    public JwtService(AuthProperties auth) {
        this.auth = auth;
        this.key = Keys.hmacShaKeyFor(auth.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Issued issue(String userId, String sessionId) {
        Instant expires = Instant.now().plus(auth.getSessionTtl());
        String token = Jwts.builder()
                .subject(userId)
                .id(sessionId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expires))
                .signWith(key)
                .compact();
        String expiresAt = Times.format(LocalDateTime.ofInstant(expires, ZoneId.systemDefault()));
        return new Issued(token, expiresAt);
    }

    public Parsed parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new Parsed(claims.getSubject(), claims.getId());
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public record Issued(String token, String expiresAt) {
    }

    public record Parsed(String userId, String sessionId) {
    }
}
