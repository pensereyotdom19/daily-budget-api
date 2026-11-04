package com.pensereyotdom19.dailybudget.recurring;

import com.pensereyotdom19.dailybudget.auth.CurrentUser;
import com.pensereyotdom19.dailybudget.recurring.RecurringDtos.RecurringRequest;
import com.pensereyotdom19.dailybudget.recurring.RecurringDtos.RecurringResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring")
public class RecurringController {
    private final RecurringService recurring;

    public RecurringController(RecurringService recurring) {
        this.recurring = recurring;
    }

    @GetMapping
    public List<RecurringResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return recurring.list(CurrentUser.id(jwt));
    }

    @PutMapping("/{id}")
    public RecurringResponse put(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody RecurringRequest body) {
        return recurring.put(CurrentUser.id(jwt), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        recurring.delete(CurrentUser.id(jwt), id);
    }
}
