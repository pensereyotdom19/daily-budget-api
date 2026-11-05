package com.pensereyotdom19.dailybudget.sync;

import com.pensereyotdom19.dailybudget.auth.CurrentUser;
import com.pensereyotdom19.dailybudget.bill.BillDtos.BillResponse;
import com.pensereyotdom19.dailybudget.bill.BillService;
import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.ExpenseResponse;
import com.pensereyotdom19.dailybudget.expense.ExpenseService;
import com.pensereyotdom19.dailybudget.income.IncomeDtos.IncomeResponse;
import com.pensereyotdom19.dailybudget.income.IncomeService;
import com.pensereyotdom19.dailybudget.recurring.RecurringDtos.RecurringResponse;
import com.pensereyotdom19.dailybudget.recurring.RecurringService;
import com.pensereyotdom19.dailybudget.settings.SettingsDtos.SettingsResponse;
import com.pensereyotdom19.dailybudget.settings.SettingsService;
import com.pensereyotdom19.dailybudget.subscription.SubscriptionDtos.SubscriptionResponse;
import com.pensereyotdom19.dailybudget.subscription.SubscriptionService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * For the phone to catch up in one request.
 * 1. First time: GET /api/sync → everything.
 * 2. Save the returned serverTime.
 * 3. Next time: GET /api/sync?since=<serverTime> → only what changed, including deletions (deleted: true).
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final SettingsService settings;
    private final BillService bills;
    private final ExpenseService expenses;
    private final IncomeService incomes;
    private final RecurringService recurring;
    private final SubscriptionService subscriptions;
    private final Clock clock;

    public SyncController(SettingsService settings, BillService bills, ExpenseService expenses,
                          IncomeService incomes, RecurringService recurring,
                          SubscriptionService subscriptions, Clock clock) {
        this.settings = settings;
        this.bills = bills;
        this.expenses = expenses;
        this.incomes = incomes;
        this.recurring = recurring;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @GetMapping
    public Changes changes(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Instant since) {
        var userId = CurrentUser.id(jwt);
        // Read the clock first, so a change saved while we're reading shows up next time instead of being missed.
        Instant serverTime = clock.instant();
        return new Changes(serverTime,
            settings.changedSince(userId, since).orElse(null),
            bills.changedSince(userId, since),
            expenses.changedSince(userId, since),
            incomes.changedSince(userId, since),
            recurring.changedSince(userId, since),
            subscriptions.changedSince(userId, since));
    }

    public record Changes(Instant serverTime, SettingsResponse settings,
                          List<BillResponse> bills, List<ExpenseResponse> expenses,
                          List<IncomeResponse> incomes, List<RecurringResponse> recurring,
                          List<SubscriptionResponse> subscriptions) {}
}
