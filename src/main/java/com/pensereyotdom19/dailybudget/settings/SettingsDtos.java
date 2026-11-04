package com.pensereyotdom19.dailybudget.settings;

import com.pensereyotdom19.dailybudget.common.MonthKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public final class SettingsDtos {
    private SettingsDtos() {}

    public record SettingsRequest(
        @NotNull @Pattern(regexp = MonthKey.PATTERN) String startMonth,
        @Min(1) @Max(24) int monthCount,
        @PositiveOrZero long totalCents,
        @PositiveOrZero long livingMonthlyCents,
        @PositiveOrZero long engagementCents,
        Instant firstOpenedAt,
        @Pattern(regexp = MonthKey.PATTERN) String historyStartMonth,
        @PositiveOrZero Long savedBeforeCents,
        // Optional: {"coffee":1500,"food":3000}. The annotations inside <...> check every key and value.
        @Size(max = CategoryLimits.MAX_ENTRIES)
        Map<@NotNull @Pattern(regexp = CategoryLimits.ALLOWED_KEYS) String,
            @NotNull @Min(0) @Max(CategoryLimits.MAX_CENTS) Long> categoryLimits) {}

    public record SettingsResponse(
        String startMonth, int monthCount, long totalCents, long livingMonthlyCents,
        long engagementCents, Instant firstOpenedAt, String historyStartMonth, long savedBeforeCents,
        Map<String, Long> categoryLimits, Instant updatedAt) {

        static SettingsResponse from(BudgetSettings s) {
            return new SettingsResponse(s.getStartMonth(), s.getMonthCount(), s.getTotalCents(),
                s.getLivingMonthlyCents(), s.getEngagementCents(), s.getFirstOpenedAt(),
                s.getHistoryStartMonth(), s.getSavedBeforeCents(),
                CategoryLimits.fromJson(s.getCategoryLimits()), s.getUpdatedAt());
        }
    }
}
