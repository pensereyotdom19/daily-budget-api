package com.pensereyotdom19.dailybudget.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reads app.jwt.secret and app.jwt.ttl from application.properties. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl) {
    public JwtProperties {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters");
        }
        if (ttl == null) ttl = Duration.ofDays(30);
    }
}
