package com.pensereyotdom19.dailybudget.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A subscription (Netflix, iCloud, …) that renews every month or every year. */
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    /** "monthly" or "yearly" (checked by the request's @Pattern). */
    @Column(nullable = false)
    private String cycle;

    /** The next time it will be charged. */
    @Column(name = "next_renewal", nullable = false)
    private Instant nextRenewal;

    /** The day of the month it renews on (1..31), so it doesn't drift after a short month. */
    @Column(name = "anchor_day", nullable = false)
    private int anchorDay;

    @Column(name = "start_month", nullable = false)
    private String startMonth;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Subscription() {}

    public Subscription(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public long getAmountCents() { return amountCents; }
    public String getCycle() { return cycle; }
    public Instant getNextRenewal() { return nextRenewal; }
    public int getAnchorDay() { return anchorDay; }
    public String getStartMonth() { return startMonth; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void update(String name, long amountCents, String cycle, Instant nextRenewal, int anchorDay,
                       String startMonth, boolean active, Instant now) {
        this.name = name;
        this.amountCents = amountCents;
        this.cycle = cycle;
        this.nextRenewal = nextRenewal;
        this.anchorDay = anchorDay;
        this.startMonth = startMonth;
        this.active = active;
        this.updatedAt = now;
        this.deletedAt = null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
