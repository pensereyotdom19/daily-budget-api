package com.pensereyotdom19.dailybudget.expense;

import com.pensereyotdom19.dailybudget.common.MonthKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ExpenseDtos {
    private ExpenseDtos() {}

    public record ExpenseRequest(
        @Positive @Max(100_000_000) long amountCents,
        @NotNull Category category,
        @Size(max = 200) String note,
        @NotNull Instant spentAt,
        UUID billId,
        @Pattern(regexp = MonthKey.PATTERN) String billMonth) {}

    public record ExpenseResponse(
        UUID id, long amountCents, Category category, String note, Instant spentAt,
        UUID billId, String billMonth, Instant updatedAt, boolean deleted) {

        static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getAmountCents(), e.getCategory(), e.getNote(), e.getSpentAt(),
                e.getBillId(), e.getBillMonth(), e.getUpdatedAt(), e.getDeletedAt() != null);
        }
    }

    /** Same split as the app's "Every month" screen. */
    public record MonthSpending(
        String month, long livingCents, long billsCents, long engagementCents, long totalCents,
        Map<Category, Long> byCategory) {}
}
