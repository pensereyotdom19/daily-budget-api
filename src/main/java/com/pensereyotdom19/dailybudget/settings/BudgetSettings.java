package com.pensereyotdom19.dailybudget.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** The 6-month plan. One row per user, keyed by the user's id. */
@Entity
@Table(name = "budget_settings")
public class BudgetSettings {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "start_month", nullable = false)
    private String startMonth;
    @Column(name = "month_count", nullable = false)
    private int monthCount;
    @Column(name = "total_cents", nullable = false)
    private long totalCents;
    @Column(name = "living_monthly_cents", nullable = false)
    private long livingMonthlyCents;
    @Column(name = "engagement_cents", nullable = false)
    private long engagementCents;
    @Column(name = "first_opened_at")
    private Instant firstOpenedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetSettings() {}

    public BudgetSettings(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public String getStartMonth() { return startMonth; }
    public int getMonthCount() { return monthCount; }
    public long getTotalCents() { return totalCents; }
    public long getLivingMonthlyCents() { return livingMonthlyCents; }
    public long getEngagementCents() { return engagementCents; }
    public Instant getFirstOpenedAt() { return firstOpenedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String startMonth, int monthCount, long totalCents, long livingMonthlyCents,
                       long engagementCents, Instant firstOpenedAt, Instant now) {
        this.startMonth = startMonth;
        this.monthCount = monthCount;
        this.totalCents = totalCents;
        this.livingMonthlyCents = livingMonthlyCents;
        this.engagementCents = engagementCents;
        this.firstOpenedAt = firstOpenedAt;
        this.updatedAt = now;
    }
}
