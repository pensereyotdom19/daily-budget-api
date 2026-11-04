package com.pensereyotdom19.dailybudget.recurring;

import com.pensereyotdom19.dailybudget.recurring.RecurringDtos.RecurringRequest;
import com.pensereyotdom19.dailybudget.recurring.RecurringDtos.RecurringResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecurringService {
    private final RecurringRepository repo;
    private final Clock clock;

    public RecurringService(RecurringRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RecurringResponse> list(UUID userId) {
        return repo.findByUserIdAndDeletedAtIsNullOrderByDayOfMonth(userId).stream().map(RecurringResponse::from).toList();
    }

    @Transactional
    public RecurringResponse put(UUID userId, UUID id, RecurringRequest r) {
        RecurringExpense item = repo.findById(id).map(x -> owned(x, userId)).orElseGet(() -> new RecurringExpense(id, userId));
        String note = r.note() == null ? "" : r.note().trim();
        item.update(r.amountCents(), r.category(), note, r.dayOfMonth(), r.startMonth(), r.lastAddedMonth(),
            r.active(), clock.instant());
        return RecurringResponse.from(repo.save(item));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        RecurringExpense item = repo.findById(id).filter(x -> x.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurring expense not found"));
        if (item.getDeletedAt() == null) item.markDeleted(clock.instant());
    }

    @Transactional(readOnly = true)
    public List<RecurringResponse> changedSince(UUID userId, Instant since) {
        List<RecurringExpense> list = since == null ? repo.findByUserId(userId) : repo.findByUserIdAndUpdatedAtAfter(userId, since);
        return list.stream().map(RecurringResponse::from).toList();
    }

    private static RecurringExpense owned(RecurringExpense r, UUID userId) {
        if (!r.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That id is already in use");
        }
        return r;
    }
}
