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
    private final LoginThrottle throttle;
    private final TokenVersionCheck versionCheck;
    /** A real bcrypt hash of a random password, checked when the email is unknown, so a wrong email
     *  takes as long as a wrong password and nobody can tell which emails have accounts. */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokens, Clock clock,
                       @Value("${app.registration-enabled}") boolean registrationEnabled, LoginThrottle throttle,
                       TokenVersionCheck versionCheck) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
        this.registrationEnabled = registrationEnabled;
        this.throttle = throttle;
        this.versionCheck = versionCheck;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public TokenResponse register(Credentials c, String address) {
        throttle.checkAndRecordRegistration(address);
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
    public TokenResponse login(Credentials c, String address) {
        String email = normalize(c.email());
        throttle.checkLogin(email, address);
        var found = users.findByEmail(email);
        boolean ok = passwordEncoder.matches(c.password(), found.map(AppUser::getPasswordHash).orElse(dummyHash))
            && found.isPresent();
        if (!ok) {
            throttle.loginFailed(email, address);
            // Same message for "no such email" and "wrong password", so nobody can probe which emails exist.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        throttle.loginSucceeded(email);
        return token(found.get());
    }

    /**
     * Sign out everywhere: raises the user's token version, so every token made before now gets 401.
     * Not one transaction around everything on purpose: the UPDATE commits first, then this server's
     * memory of the version is updated, so no request can read the old number back in between.
     */
    public void logoutAll(UUID userId) {
        users.bumpTokenVersion(userId);
        users.findTokenVersionById(userId).ifPresent(v -> versionCheck.remember(userId, v));
    }

    private TokenResponse token(AppUser user) {
        TokenService.IssuedToken t = tokens.issue(user.getId(), user.getTokenVersion());
        return new TokenResponse(t.value(), t.expiresAt(), user.getId());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
