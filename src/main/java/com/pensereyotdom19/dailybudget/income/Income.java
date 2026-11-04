package com.pensereyotdom19.dailybudget.income;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Extra money received (a gift, a side job), and where it went. */
@Entity
@Table(name = "incomes")
public class Income {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    /** "living", "savings" or "engagement", the same names as the app. */
    @Column(nullable = false)
    private String target;

    @Column(nullable = false)
    private String note;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Income() {}

    public Income(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public long getAmountCents() { return amountCents; }
    public String getTarget() { return target; }
    public String getNote() { return note; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void update(long amountCents, String target, String note, Instant receivedAt, Instant now) {
        this.amountCents = amountCents;
        this.target = target;
        this.note = note;
        this.receivedAt = receivedAt;
        this.updatedAt = now;
        this.deletedAt = null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
