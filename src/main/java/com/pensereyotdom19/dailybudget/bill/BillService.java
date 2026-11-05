package com.pensereyotdom19.dailybudget.bill;

import com.pensereyotdom19.dailybudget.bill.BillDtos.BillRequest;
import com.pensereyotdom19.dailybudget.bill.BillDtos.BillResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillService {
    private final BillRepository repo;
    private final Clock clock;

    public BillService(BillRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BillResponse> list(UUID userId) {
        return repo.findByUserIdAndDeletedAtIsNullOrderBySortOrder(userId).stream().map(BillResponse::from).toList();
    }

    /** Create or replace the bill with this id (the phone makes the id). */
    @Transactional
    public BillResponse put(UUID userId, UUID id, BillRequest r) {
        Bill bill = repo.findById(id).map(b -> owned(b, userId)).orElseGet(() -> new Bill(id, userId));
        bill.update(r.name().trim(), r.amountCents(), r.sortOrder(), r.dueMonths(), r.paidMonths(),
            r.dueDay(), r.lastAutoMonth(), clock.instant());
        return BillResponse.from(repo.save(bill));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Bill bill = repo.findById(id).filter(b -> b.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        if (bill.getDeletedAt() == null) bill.markDeleted(clock.instant());
    }

    @Transactional(readOnly = true)
    public List<BillResponse> changedSince(UUID userId, Instant since) {
        List<Bill> bills = since == null ? repo.findByUserId(userId) : repo.findByUserIdAndUpdatedAtAfter(userId, since);
        return bills.stream().map(BillResponse::from).toList();
    }

    /** Ids are global, so an id that belongs to someone else is refused (without saying whose it is). */
    private static Bill owned(Bill b, UUID userId) {
        if (!b.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That id is already in use");
        }
        return b;
    }
}
