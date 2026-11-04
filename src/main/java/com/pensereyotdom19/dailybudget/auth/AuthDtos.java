package com.pensereyotdom19.dailybudget.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** JSON shapes for /api/auth. A record is a small immutable class: fields, constructor and getters in one line. */
public final class AuthDtos {
    private AuthDtos() {}

    public record Credentials(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 72) String password) {}

    public record TokenResponse(String token, Instant expiresAt, UUID userId) {}
}
