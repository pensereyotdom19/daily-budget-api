package com.pensereyotdom19.dailybudget.subscription;

import com.pensereyotdom19.dailybudget.subscription.SubscriptionDtos.SubscriptionRequest;
import com.pensereyotdom19.dailybudget.subscription.SubscriptionDtos.SubscriptionResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {
    private final SubscriptionRepository repo;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list(UUID userId) {
        return repo.findByUserIdAndDeletedAtIsNullOrderByNextRenewal(userId).stream().map(SubscriptionResponse::from).toList();
    }

    /** Create or replace the subscription with this id (the phone makes the id). */
    @Transactional
    public SubscriptionResponse put(UUID userId, UUID id, SubscriptionRequest r) {
        Subscription item = repo.findById(id).map(x -> owned(x, userId)).orElseGet(() -> new Subscription(id, userId));
        item.update(r.name().trim(), r.amountCents(), r.cycle(), r.nextRenewal(), r.anchorDay(), r.startMonth(),
            r.active(), clock.instant());
        return SubscriptionResponse.from(repo.save(item));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Subscription item = repo.findById(id).filter(x -> x.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (item.getDeletedAt() == null) item.markDeleted(clock.instant());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> changedSince(UUID userId, Instant since) {
        List<Subscription> list = since == null ? repo.findByUserId(userId) : repo.findByUserIdAndUpdatedAtAfter(userId, since);
        return list.stream().map(SubscriptionResponse::from).toList();
    }

    /** Someone else's id: refuse instead of overwriting their row. */
    private static Subscription owned(Subscription s, UUID userId) {
        if (!s.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That id is already in use");
        }
        return s;
    }
}
