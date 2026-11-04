package com.pensereyotdom19.dailybudget.income;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class IncomeDtos {
    private IncomeDtos() {}

    public record IncomeRequest(
        @Positive @Max(100_000_000) long amountCents,
        @NotNull @Pattern(regexp = "^(living|savings|engagement)$") String target,
        @Size(max = 200) String note,
        @NotNull Instant receivedAt) {}

    public record IncomeResponse(
        UUID id, long amountCents, String target, String note, Instant receivedAt,
        Instant updatedAt, boolean deleted) {

        static IncomeResponse from(Income i) {
            return new IncomeResponse(i.getId(), i.getAmountCents(), i.getTarget(), i.getNote(), i.getReceivedAt(),
                i.getUpdatedAt(), i.getDeletedAt() != null);
        }
    }
}
