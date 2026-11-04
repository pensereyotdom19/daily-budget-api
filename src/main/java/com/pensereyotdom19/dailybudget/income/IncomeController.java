package com.pensereyotdom19.dailybudget.income;

import com.pensereyotdom19.dailybudget.auth.CurrentUser;
import com.pensereyotdom19.dailybudget.income.IncomeDtos.IncomeRequest;
import com.pensereyotdom19.dailybudget.income.IncomeDtos.IncomeResponse;
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
@RequestMapping("/api/incomes")
public class IncomeController {
    private final IncomeService incomes;

    public IncomeController(IncomeService incomes) {
        this.incomes = incomes;
    }

    @GetMapping
    public List<IncomeResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return incomes.list(CurrentUser.id(jwt));
    }

    @PutMapping("/{id}")
    public IncomeResponse put(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody IncomeRequest body) {
        return incomes.put(CurrentUser.id(jwt), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        incomes.delete(CurrentUser.id(jwt), id);
    }
}
