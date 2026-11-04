package com.pensereyotdom19.dailybudget.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Who may call what.
 * - /api/auth/** (register, login) and the health check are open.
 * - The web dashboard's static files (index.html, app.js, app.css, favicon.svg) are open: they hold
 *   no data. The page logs in and then calls /api with a token, like the phone does.
 * - Everything else needs "Authorization: Bearer <token>" from login.
 * No cookies or sessions: the phone sends the token with every request.
 *
 * Security headers (sent on every response) tell the browser to lock the page down:
 * - Content-Security-Policy: only load scripts, styles and API calls from this same server,
 *   and never run inline scripts. If someone sneaks HTML into a note, it can't run.
 * - X-Frame-Options DENY + frame-ancestors 'none': no other site may show this page in a frame.
 * - X-Content-Type-Options nosniff: the browser trusts our Content-Type instead of guessing.
 * - Referrer-Policy no-referrer: links don't leak this server's URLs to other sites.
 */
@Configuration
public class SecurityConfig {

    /** The dashboard's files in src/main/resources/static. Only these are public, nothing else. */
    static final String[] DASHBOARD_FILES = {"/", "/index.html", "/app.js", "/app.css", "/favicon.svg"};

    static final String CONTENT_SECURITY_POLICY = String.join("; ",
        "default-src 'none'",
        "script-src 'self'",
        "style-src 'self'",
        "connect-src 'self'",
        "img-src 'self'",
        "base-uri 'none'",
        "form-action 'self'",
        "frame-ancestors 'none'");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF protects cookie logins; we use tokens instead
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, DASHBOARD_FILES).permitAll()
                .anyRequest().authenticated())
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .frameOptions(f -> f.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                // Browsers must use HTTPS for a year, and the dashboard may not use camera, mic or location.
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()")))
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    SecretKey jwtKey(JwtProperties props) {
        return new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Signs tokens at login. */
    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /** Checks the signature, expiry and issuer of tokens on every request. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
