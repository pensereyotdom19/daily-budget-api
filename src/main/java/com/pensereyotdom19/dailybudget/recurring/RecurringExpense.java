package com.pensereyotdom19.dailybudget.recurring;

import com.pensereyotdom19.dailybudget.expense.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An expense the app adds every month on the same day, like a phone top-up. */
@Entity
@Table(name = "recurring_expenses")
public class RecurringExpense {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String note;

    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(name = "start_month", nullable = false)
    private String startMonth;

    /** The last month the app added it for, so it is never added twice. */
    @Column(name = "last_added_month")
    private String lastAddedMonth;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected RecurringExpense() {}

    public RecurringExpense(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public long getAmountCents() { return amountCents; }
    public Category getCategory() { return category; }
    public String getNote() { return note; }
    public int getDayOfMonth() { return dayOfMonth; }
    public String getStartMonth() { return startMonth; }
    public String getLastAddedMonth() { return lastAddedMonth; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void update(long amountCents, Category category, String note, int dayOfMonth, String startMonth,
                       String lastAddedMonth, boolean active, Instant now) {
        this.amountCents = amountCents;
        this.category = category;
        this.note = note;
        this.dayOfMonth = dayOfMonth;
        this.startMonth = startMonth;
        this.lastAddedMonth = lastAddedMonth;
        this.active = active;
        this.updatedAt = now;
        this.deletedAt = null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
