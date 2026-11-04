package com.pensereyotdom19.dailybudget.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A person who can log in. Named AppUser because "User" clashes with Spring Security's class. */
@Entity
@Table(name = "users")
public class AppUser {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash. The real password is never stored. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Goes up by one on "sign out everywhere". Tokens made with an older number stop working. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    protected AppUser() {} // JPA needs an empty constructor

    public AppUser(UUID id, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public int getTokenVersion() { return tokenVersion; }
}
