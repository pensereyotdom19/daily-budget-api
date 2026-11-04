package com.pensereyotdom19.dailybudget.expense;

import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.ExpenseRequest;
import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.ExpenseResponse;
import com.pensereyotdom19.dailybudget.expense.ExpenseDtos.MonthSpending;
import com.pensereyotdom19.dailybudget.expense.ExpenseRepository.CategoryTotal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseService {
    private final ExpenseRepository repo;
    private final Clock clock;

    public ExpenseService(ExpenseRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID userId, Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'");
        }
        return repo.findByUserIdAndDeletedAtIsNullAndSpentAtGreaterThanEqualAndSpentAtLessThanOrderBySpentAtDesc(userId, from, to)
            .stream().map(ExpenseResponse::from).toList();
    }

    @Transactional
    public ExpenseResponse put(UUID userId, UUID id, ExpenseRequest r) {
        if ((r.billId() == null) != (r.billMonth() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "billId and billMonth go together");
        }
        Expense e = repo.findById(id).map(x -> owned(x, userId)).orElseGet(() -> new Expense(id, userId));
        String note = r.note() == null ? "" : r.note().trim();
        e.update(r.amountCents(), r.category(), note, r.spentAt(), r.billId(), r.billMonth(), clock.instant());
        return ExpenseResponse.from(repo.save(e));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Expense e = repo.findById(id).filter(x -> x.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        if (e.getDeletedAt() == null) e.markDeleted(clock.instant());
    }

    /**
     * How much was spent in a month, split like the app: living money, bills, engagement.
     * The month starts at midnight in the phone's time zone, so the zone matters.
     */
    @Transactional(readOnly = true)
    public MonthSpending monthSpending(UUID userId, YearMonth month, ZoneId zone) {
        Instant from = month.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        Map<Category, Long> byCategory = new EnumMap<>(Category.class);
        long living = 0, bills = 0, engagement = 0;
        for (CategoryTotal t : repo.totalsByCategory(userId, from, to)) {
            long cents = t.getTotalCents();
            byCategory.put(t.getCategory(), cents);
            if (t.getCategory() == Category.BILLS) bills += cents;
            else if (t.getCategory() == Category.ENGAGEMENT) engagement += cents;
            else living += cents;
        }
        return new MonthSpending(month.toString(), living, bills, engagement, living + bills + engagement, byCategory);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> changedSince(UUID userId, Instant since) {
        List<Expense> list = since == null ? repo.findByUserId(userId) : repo.findByUserIdAndUpdatedAtAfter(userId, since);
        return list.stream().map(ExpenseResponse::from).toList();
    }

    private static Expense owned(Expense e, UUID userId) {
        if (!e.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That id is already in use");
        }
        return e;
    }
}
