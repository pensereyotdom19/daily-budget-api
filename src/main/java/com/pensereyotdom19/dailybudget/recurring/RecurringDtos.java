package com.pensereyotdom19.dailybudget.recurring;

import com.pensereyotdom19.dailybudget.common.MonthKey;
import com.pensereyotdom19.dailybudget.expense.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class RecurringDtos {
    private RecurringDtos() {}

    public record RecurringRequest(
        @Positive @Max(100_000_000) long amountCents,
        @NotNull Category category,
        @Size(max = 200) String note,
        @Min(1) @Max(31) int dayOfMonth,
        @NotNull @Pattern(regexp = MonthKey.PATTERN) String startMonth,
        @Pattern(regexp = MonthKey.PATTERN) String lastAddedMonth,
        boolean active) {}

    public record RecurringResponse(
        UUID id, long amountCents, Category category, String note, int dayOfMonth, String startMonth,
        String lastAddedMonth, boolean active, Instant updatedAt, boolean deleted) {

        static RecurringResponse from(RecurringExpense r) {
            return new RecurringResponse(r.getId(), r.getAmountCents(), r.getCategory(), r.getNote(), r.getDayOfMonth(),
                r.getStartMonth(), r.getLastAddedMonth(), r.isActive(), r.getUpdatedAt(), r.getDeletedAt() != null);
        }
    }
}
