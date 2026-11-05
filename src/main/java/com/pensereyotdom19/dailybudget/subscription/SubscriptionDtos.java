package com.pensereyotdom19.dailybudget.subscription;

import com.pensereyotdom19.dailybudget.common.MonthKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class SubscriptionDtos {
    private SubscriptionDtos() {}

    public record SubscriptionRequest(
        @Positive @Max(100_000_000) long amountCents,
        @NotBlank @Size(max = 60) String name,
        @NotNull @Pattern(regexp = "^(monthly|yearly)$") String cycle,
        @NotNull Instant nextRenewal,
        @Min(1) @Max(31) int anchorDay,
        @NotNull @Pattern(regexp = MonthKey.PATTERN) String startMonth,
        boolean active) {}

    public record SubscriptionResponse(
        UUID id, long amountCents, String name, String cycle, Instant nextRenewal, int anchorDay,
        String startMonth, boolean active, Instant updatedAt, boolean deleted) {

        static SubscriptionResponse from(Subscription s) {
            return new SubscriptionResponse(s.getId(), s.getAmountCents(), s.getName(), s.getCycle(), s.getNextRenewal(),
                s.getAnchorDay(), s.getStartMonth(), s.isActive(), s.getUpdatedAt(), s.getDeletedAt() != null);
        }
    }
}
