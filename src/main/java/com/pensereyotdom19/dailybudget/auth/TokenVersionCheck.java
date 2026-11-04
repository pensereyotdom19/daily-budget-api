package com.pensereyotdom19.dailybudget.auth;

import com.pensereyotdom19.dailybudget.user.AppUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Makes "sign out everywhere" work.
 *
 * A JWT can't be taken back once it is handed out: the server only checks its signature. So every token
 * carries a number, the "ver" claim, copied from the user's token_version when the token was made.
 * Sign out everywhere adds one to token_version, and this check refuses any token whose number no
 * longer matches (Spring then answers 401). It also refuses tokens of users who no longer exist.
 * Tokens from before this feature have no "ver" claim; they count as version 0.
 *
 * Spring Security runs this on every request with a token, after it has checked the signature,
 * expiry and issuer (see SecurityConfig.jwtDecoder).
 *
 * To avoid a database query on every request, versions are remembered for 30 seconds. Sign out
 * everywhere updates this memory straight away (see remember), so on this server it takes effect at once.
 */
@Component
public class TokenVersionCheck implements OAuth2TokenValidator<Jwt> {
    static final String CLAIM = "ver";
    private static final Duration CACHE_TIME = Duration.ofSeconds(30);
    /** Stored for a user who doesn't exist. Real versions are never negative. */
    private static final int NO_USER = -1;

    private final AppUserRepository users;
    private final Clock clock;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(int version, Instant until) {}

    public TokenVersionCheck(AppUserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException e) { // missing or not a UUID
            return refuse();
        }
        Object claim = jwt.getClaims().get(CLAIM);
        int tokenVersion;
        if (claim == null) {
            tokenVersion = 0;
        } else if (claim instanceof Number n) {
            tokenVersion = n.intValue();
        } else {
            return refuse();
        }
        int current = currentVersion(userId);
        if (current == NO_USER || current != tokenVersion) {
            return refuse();
        }
        return OAuth2TokenValidatorResult.success();
    }

    /** The user's token version now, from memory if it was read in the last 30 seconds. */
    private int currentVersion(UUID userId) {
        Instant now = clock.instant();
        Cached hit = cache.get(userId);
        if (hit != null && hit.until().isAfter(now)) {
            return hit.version();
        }
        int version = users.findTokenVersionById(userId).orElse(NO_USER);
        remember(userId, version);
        if (cache.size() > 10_000) {
            cache.values().removeIf(c -> !c.until().isAfter(now));
        }
        return cache.get(userId).version();
    }

    /**
     * Saves a version in memory. Versions only ever go up, so if two requests race, the higher number wins:
     * a slow request that read the old number can't put it back after sign out everywhere.
     * An entry that has run out is always replaced.
     */
    void remember(UUID userId, int version) {
        Instant now = clock.instant();
        Cached fresh = new Cached(version, now.plus(CACHE_TIME));
        cache.merge(userId, fresh,
            (old, neu) -> old.until().isAfter(now) && old.version() > neu.version() ? old : neu);
    }

    private static OAuth2TokenValidatorResult refuse() {
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "This sign-in has ended. Please sign in again.", null));
    }
}
