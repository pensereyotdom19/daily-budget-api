package com.pensereyotdom19.dailybudget.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Slows down password guessing.
 * - An account is locked for the rest of a 15-minute window after 5 wrong passwords.
 * - One network address gets 20 wrong passwords per window, whatever emails it tries.
 * - One network address gets a few sign-up attempts per hour.
 * Kept in memory: a restart clears it, which is fine for one small server.
 */
@Service
public class LoginThrottle {
    private final Clock clock;
    private final int failuresPerAccount;
    private final int failuresPerAddress;
    private final int registrationsPerAddress;
    private final Duration window;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    public LoginThrottle(Clock clock,
                         @Value("${app.throttle.failures-per-account:5}") int failuresPerAccount,
                         @Value("${app.throttle.failures-per-address:20}") int failuresPerAddress,
                         @Value("${app.throttle.registrations-per-address:5}") int registrationsPerAddress,
                         @Value("${app.throttle.window:PT15M}") Duration window) {
        this.clock = clock;
        this.failuresPerAccount = failuresPerAccount;
        this.failuresPerAddress = failuresPerAddress;
        this.registrationsPerAddress = registrationsPerAddress;
        this.window = window;
    }

    /** Throws 429 if this email or address has failed too often recently. */
    public void checkLogin(String email, String address) {
        refuseIfOver("fail:email:" + email, failuresPerAccount, window);
        refuseIfOver("fail:ip:" + address, failuresPerAddress, window);
    }

    public void loginFailed(String email, String address) {
        record("fail:email:" + email);
        record("fail:ip:" + address);
    }

    public void loginSucceeded(String email) {
        events.remove("fail:email:" + email);
    }

    /** Counts every sign-up attempt, successful or not. */
    public void checkAndRecordRegistration(String address) {
        refuseIfOver("register:ip:" + address, registrationsPerAddress, Duration.ofHours(1));
        record("register:ip:" + address);
    }

    private void record(String key) {
        Instant now = clock.instant();
        events.compute(key, (k, q) -> {
            Deque<Instant> list = q == null ? new ArrayDeque<>() : q;
            synchronized (list) {
                list.addLast(now);
                while (list.size() > 100) list.removeFirst();
            }
            return list;
        });
        if (events.size() > 10_000) prune(now);
    }

    private void refuseIfOver(String key, int limit, Duration period) {
        Instant now = clock.instant();
        Deque<Instant> q = events.get(key);
        if (q == null) return;
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(now.minus(period))) q.removeFirst();
            if (q.size() >= limit) {
                long wait = Math.max(1, Duration.between(now, q.peekFirst().plus(period)).toMinutes() + 1);
                throw new TooManyAttempts(
                    "Too many attempts. Try again in " + wait + " minute" + (wait == 1 ? "" : "s") + ".", wait * 60);
            }
        }
    }

    private void prune(Instant now) {
        Instant oldest = now.minus(Duration.ofHours(1));
        events.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast().isBefore(oldest));
    }

    /** 429 with a Retry-After header, so clients know how long to wait. */
    public static class TooManyAttempts extends ResponseStatusException {
        private final long retryAfterSeconds;

        TooManyAttempts(String reason, long retryAfterSeconds) {
            super(HttpStatus.TOO_MANY_REQUESTS, reason);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders h = new HttpHeaders();
            h.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
            return h;
        }
    }
}
