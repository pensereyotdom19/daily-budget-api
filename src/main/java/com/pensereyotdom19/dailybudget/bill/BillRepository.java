package com.pensereyotdom19.dailybudget.bill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, UUID> {
    List<Bill> findByUserIdAndDeletedAtIsNullOrderBySortOrder(UUID userId);
    List<Bill> findByUserIdAndUpdatedAtAfter(UUID userId, Instant since);
    List<Bill> findByUserId(UUID userId);
}
