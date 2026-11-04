package com.pensereyotdom19.dailybudget.settings;

import com.pensereyotdom19.dailybudget.auth.CurrentUser;
import com.pensereyotdom19.dailybudget.settings.SettingsDtos.SettingsRequest;
import com.pensereyotdom19.dailybudget.settings.SettingsDtos.SettingsResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settings;

    public SettingsController(SettingsService settings) {
        this.settings = settings;
    }

    /** GET /api/settings → the plan, or 404 if the app hasn't uploaded one yet. */
    @GetMapping
    public SettingsResponse get(@AuthenticationPrincipal Jwt jwt) {
        return settings.get(CurrentUser.id(jwt))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No plan saved yet"));
    }

    /** PUT /api/settings → saves the whole plan. */
    @PutMapping
    public SettingsResponse put(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SettingsRequest body) {
        return settings.put(CurrentUser.id(jwt), body);
    }
}
