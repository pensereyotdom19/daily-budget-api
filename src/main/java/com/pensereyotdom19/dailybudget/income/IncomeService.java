package com.pensereyotdom19.dailybudget.income;

import com.pensereyotdom19.dailybudget.income.IncomeDtos.IncomeRequest;
import com.pensereyotdom19.dailybudget.income.IncomeDtos.IncomeResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IncomeService {
    private final IncomeRepository repo;
    private final Clock clock;

    public IncomeService(IncomeRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> list(UUID userId) {
        return repo.findByUserIdAndDeletedAtIsNullOrderByReceivedAtDesc(userId).stream().map(IncomeResponse::from).toList();
    }

    /** Create or replace the income with this id (the phone makes the id). */
    @Transactional
    public IncomeResponse put(UUID userId, UUID id, IncomeRequest r) {
        Income income = repo.findById(id).map(i -> owned(i, userId)).orElseGet(() -> new Income(id, userId));
        String note = r.note() == null ? "" : r.note().trim();
        income.update(r.amountCents(), r.target(), note, r.receivedAt(), clock.instant());
        return IncomeResponse.from(repo.save(income));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Income income = repo.findById(id).filter(i -> i.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Income not found"));
        if (income.getDeletedAt() == null) income.markDeleted(clock.instant());
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> changedSince(UUID userId, Instant since) {
        List<Income> list = since == null ? repo.findByUserId(userId) : repo.findByUserIdAndUpdatedAtAfter(userId, since);
        return list.stream().map(IncomeResponse::from).toList();
    }

    private static Income owned(Income i, UUID userId) {
        if (!i.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That id is already in use");
        }
        return i;
    }
}
