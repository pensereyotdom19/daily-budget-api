package com.pensereyotdom19.dailybudget.settings;

import com.pensereyotdom19.dailybudget.common.MonthKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public final class SettingsDtos {
    private SettingsDtos() {}

    public record SettingsRequest(
        @NotNull @Pattern(regexp = MonthKey.PATTERN) String startMonth,
        @Min(1) @Max(24) int monthCount,
        @PositiveOrZero long totalCents,
        @PositiveOrZero long livingMonthlyCents,
        @PositiveOrZero long engagementCents,
        Instant firstOpenedAt) {}

    public record SettingsResponse(
        String startMonth, int monthCount, long totalCents, long livingMonthlyCents,
        long engagementCents, Instant firstOpenedAt, Instant updatedAt) {

        static SettingsResponse from(BudgetSettings s) {
            return new SettingsResponse(s.getStartMonth(), s.getMonthCount(), s.getTotalCents(),
                s.getLivingMonthlyCents(), s.getEngagementCents(), s.getFirstOpenedAt(), s.getUpdatedAt());
        }
    }
}
