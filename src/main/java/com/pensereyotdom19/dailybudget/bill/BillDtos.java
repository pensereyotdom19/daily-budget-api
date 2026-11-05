package com.pensereyotdom19.dailybudget.bill;

import com.pensereyotdom19.dailybudget.common.MonthKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class BillDtos {
    private BillDtos() {}

    public record BillRequest(
        @NotBlank @Size(max = 60) String name,
        @PositiveOrZero @Max(100_000_000) long amountCents,
        int sortOrder,
        @NotNull @Size(max = 60) Set<@Pattern(regexp = MonthKey.PATTERN) String> dueMonths,
        @NotNull @Size(max = 60) Set<@Pattern(regexp = MonthKey.PATTERN) String> paidMonths,
        @Min(1) @Max(31) Integer dueDay,
        @Pattern(regexp = MonthKey.PATTERN) String lastAutoMonth) {}

    public record BillResponse(
        UUID id, String name, long amountCents, int sortOrder,
        Set<String> dueMonths, Set<String> paidMonths, Integer dueDay, String lastAutoMonth,
        Instant updatedAt, boolean deleted) {

        static BillResponse from(Bill b) {
            // TreeSet = months in order, so the JSON is easy to read.
            return new BillResponse(b.getId(), b.getName(), b.getAmountCents(), b.getSortOrder(),
                new TreeSet<>(b.getDueMonths()), new TreeSet<>(b.getPaidMonths()),
                b.getDueDay(), b.getLastAutoMonth(), b.getUpdatedAt(), b.getDeletedAt() != null);
        }
    }
}
