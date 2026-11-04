package com.pensereyotdom19.dailybudget.recurring;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringRepository extends JpaRepository<RecurringExpense, UUID> {
    List<RecurringExpense> findByUserIdAndDeletedAtIsNullOrderByDayOfMonth(UUID userId);
    List<RecurringExpense> findByUserIdAndUpdatedAtAfter(UUID userId, Instant since);
    List<RecurringExpense> findByUserId(UUID userId);
}
