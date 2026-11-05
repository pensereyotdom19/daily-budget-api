package com.pensereyotdom19.dailybudget.subscription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    /** The ones not deleted, soonest renewal first. */
    List<Subscription> findByUserIdAndDeletedAtIsNullOrderByNextRenewal(UUID userId);
    List<Subscription> findByUserIdAndUpdatedAtAfter(UUID userId, Instant since);
    List<Subscription> findByUserId(UUID userId);
}
