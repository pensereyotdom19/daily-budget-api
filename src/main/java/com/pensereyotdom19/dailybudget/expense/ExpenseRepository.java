package com.pensereyotdom19.dailybudget.expense;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /** Expenses in [from, to), newest first. Method name → SQL, written by Spring. */
    List<Expense> findByUserIdAndDeletedAtIsNullAndSpentAtGreaterThanEqualAndSpentAtLessThanOrderBySpentAtDesc(
        UUID userId, Instant from, Instant to);

    List<Expense> findByUserIdAndUpdatedAtAfter(UUID userId, Instant since);

    List<Expense> findByUserId(UUID userId);

    /** A hand-written query (JPQL: like SQL, but over entities): total spent per category. */
    @Query("""
        select e.category as category, sum(e.amountCents) as totalCents
        from Expense e
        where e.userId = :userId and e.deletedAt is null
          and e.spentAt >= :from and e.spentAt < :to
        group by e.category
        """)
    List<CategoryTotal> totalsByCategory(@Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    /** Spring fills this interface from the query's "as" names. */
    interface CategoryTotal {
        Category getCategory();
        Long getTotalCents();
    }
}
