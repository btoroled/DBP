package com.streakstudy.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streakstudy.infrastructure.persistence.entity.UserJpa;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpa, Long> {
    /** Lookup cross-tenant intencional (solo para login). */
    Optional<UserJpa> findByEmail(String email);

    /** Lookup tenant-aware: garantiza que el id pertenece al tenant declarado. */
    Optional<UserJpa> findByIdAndInstitutionId(Long id, Long institutionId);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE UserJpa u SET u.streakFreezes = u.streakFreezes - 1 WHERE u.currentStreak > 0 AND u.streakFreezes > 0 AND (u.lastActiveDate IS NULL OR u.lastActiveDate < :threshold)")
    int consumeStreakFreezes(@Param("threshold") LocalDate threshold);

    @Modifying
    @Query("UPDATE UserJpa u SET u.currentStreak = 0 WHERE u.currentStreak > 0 AND u.streakFreezes = 0 AND (u.lastActiveDate IS NULL OR u.lastActiveDate < :threshold)")
    int resetUnprotectedStreaks(@Param("threshold") LocalDate threshold);
}
