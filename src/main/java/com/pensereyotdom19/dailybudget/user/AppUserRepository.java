package com.pensereyotdom19.dailybudget.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Spring writes the SQL for these methods from their names. */
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Reads only the token version, not the whole user (this runs on every request). Empty if the user is gone. */
    @Query("select u.tokenVersion from AppUser u where u.id = :id")
    Optional<Integer> findTokenVersionById(@Param("id") UUID id);

    /** Adds one to the user's token version in a single SQL UPDATE. Returns how many rows changed (0 or 1). */
    @Transactional
    @Modifying
    @Query("update AppUser u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    int bumpTokenVersion(@Param("id") UUID id);
}
