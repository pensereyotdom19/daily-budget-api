package com.pensereyotdom19.dailybudget.auth;

import com.pensereyotdom19.dailybudget.auth.AuthDtos.Credentials;
import com.pensereyotdom19.dailybudget.auth.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** POST /api/auth/register {"email": "...", "password": "..."} → 201 with a token. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody Credentials body) {
        return auth.register(body);
    }

    /** POST /api/auth/login {"email": "...", "password": "..."} → 200 with a token. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credentials body) {
        return auth.login(body);
    }
}
