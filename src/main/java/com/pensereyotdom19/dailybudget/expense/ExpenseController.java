package com.pensereyotdom19.dailybudget.expense;

import com.pensereyotdom19.dailybudget.auth.CurrentUser;
import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.ExpenseRequest;
import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.ExpenseResponse;
import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.MonthSpending;
import jakarta.validation.Valid;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    private final ExpenseService expenses;

    public ExpenseController(ExpenseService expenses) {
        this.expenses = expenses;
    }

    /** GET /api/expenses?from=2026-11-01T00:00:00Z&to=2026-12-01T00:00:00Z */
    @GetMapping
    public List<ExpenseResponse> list(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam Instant from, @RequestParam Instant to) {
        return expenses.list(CurrentUser.id(jwt), from, to);
    }

    @PutMapping("/{id}")
    public ExpenseResponse put(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody ExpenseRequest body) {
        return expenses.put(CurrentUser.id(jwt), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        expenses.delete(CurrentUser.id(jwt), id);
    }

    /** GET /api/expenses/months/2026-11?zone=Asia/Phnom_Penh → totals for that month. */
    @GetMapping("/months/{month}")
    public MonthSpending month(@AuthenticationPrincipal Jwt jwt, @PathVariable String month,
                               @RequestParam(defaultValue = "UTC") String zone) {
        try {
            return expenses.monthSpending(CurrentUser.id(jwt), YearMonth.parse(month), ZoneId.of(zone));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must look like 2026-11");
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown time zone: " + zone);
        }
    }
}
