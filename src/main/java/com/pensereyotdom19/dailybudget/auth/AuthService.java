package com.pensereyotdom19.dailybudget.auth;

import com.pensereyotdom19.dailybudget.auth.AuthDtos.Credentials;
import com.pensereyotdom19.dailybudget.auth.AuthDtos.TokenResponse;
import com.pensereyotdom19.dailybudget.user.AppUser;
import com.pensereyotdom19.dailybudget.user.AppUserRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final Clock clock;
    private final boolean registrationEnabled;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokens, Clock clock,
                       @Value("${app.registration-enabled}") boolean registrationEnabled) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
        this.registrationEnabled = registrationEnabled;
    }

    @Transactional
    public TokenResponse register(Credentials c) {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is closed");
        }
        String email = normalize(c.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email already has an account");
        }
        AppUser user = users.save(new AppUser(UUID.randomUUID(), email, passwordEncoder.encode(c.password()), clock.instant()));
        return token(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(Credentials c) {
        AppUser user = users.findByEmail(normalize(c.email()))
            .filter(u -> passwordEncoder.matches(c.password(), u.getPasswordHash()))
            // Same message for "no such email" and "wrong password", so nobody can probe which emails exist.
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return token(user);
    }

    private TokenResponse token(AppUser user) {
        TokenService.IssuedToken t = tokens.issue(user.getId());
        return new TokenResponse(t.value(), t.expiresAt(), user.getId());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
