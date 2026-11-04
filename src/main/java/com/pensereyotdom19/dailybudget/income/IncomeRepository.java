package com.pensereyotdom19.dailybudget.income;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeRepository extends JpaRepository<Income, UUID> {
    List<Income> findByUserIdAndDeletedAtIsNullOrderByReceivedAtDesc(UUID userId);
    List<Income> findByUserIdAndUpdatedAtAfter(UUID userId, Instant since);
    List<Income> findByUserId(UUID userId);
}
