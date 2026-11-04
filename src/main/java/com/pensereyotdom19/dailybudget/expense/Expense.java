package com.pensereyotdom19.dailybudget.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One thing you spent money on. */
@Entity
@Table(name = "expenses")
public class Expense {
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

    @Column(name = "spent_at", nullable = false)
    private Instant spentAt;

    /** Set when this expense is a bill payment. */
    @Column(name = "bill_id")
    private UUID billId;

    @Column(name = "bill_month")
    private String billMonth;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Expense() {}

    public Expense(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public long getAmountCents() { return amountCents; }
    public Category getCategory() { return category; }
    public String getNote() { return note; }
    public Instant getSpentAt() { return spentAt; }
    public UUID getBillId() { return billId; }
    public String getBillMonth() { return billMonth; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void update(long amountCents, Category category, String note, Instant spentAt,
                       UUID billId, String billMonth, Instant now) {
        this.amountCents = amountCents;
        this.category = category;
        this.note = note;
        this.spentAt = spentAt;
        this.billId = billId;
        this.billMonth = billMonth;
        this.updatedAt = now;
        this.deletedAt = null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
