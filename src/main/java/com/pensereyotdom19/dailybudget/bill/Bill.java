package com.pensereyotdom19.dailybudget.bill;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A planned bill, like "Electricity $100 every month". */
@Entity
@Table(name = "bills")
public class Bill {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Stored in its own table (bill_due_months), one row per month. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bill_due_months", joinColumns = @JoinColumn(name = "bill_id"))
    @Column(name = "month_key")
    private Set<String> dueMonths = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bill_paid_months", joinColumns = @JoinColumn(name = "bill_id"))
    @Column(name = "month_key")
    private Set<String> paidMonths = new HashSet<>();

    /** Day of the month it is paid automatically (1–31), or null to tick it by hand. */
    @Column(name = "due_day")
    private Integer dueDay;

    /** The last month the app paid it automatically ("2026-11"). */
    @Column(name = "last_auto_month")
    private String lastAutoMonth;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Bill() {}

    public Bill(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public long getAmountCents() { return amountCents; }
    public int getSortOrder() { return sortOrder; }
    public Set<String> getDueMonths() { return dueMonths; }
    public Set<String> getPaidMonths() { return paidMonths; }
    public Integer getDueDay() { return dueDay; }
    public String getLastAutoMonth() { return lastAutoMonth; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void update(String name, long amountCents, int sortOrder, Set<String> due, Set<String> paid,
                       Integer dueDay, String lastAutoMonth, Instant now) {
        this.dueDay = dueDay;
        this.lastAutoMonth = lastAutoMonth;
        this.name = name;
        this.amountCents = amountCents;
        this.sortOrder = sortOrder;
        this.dueMonths.clear();
        this.dueMonths.addAll(due);
        this.paidMonths.clear();
        this.paidMonths.addAll(paid);
        this.updatedAt = now;
        this.deletedAt = null; // saving a deleted bill brings it back
    }

    /** Soft delete: the row stays so other devices can sync the deletion. */
    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
