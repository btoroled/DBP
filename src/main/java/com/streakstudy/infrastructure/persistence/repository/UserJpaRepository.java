package com.streakstudy.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
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

    @Query("SELECT u FROM UserJpa u WHERE u.institutionId = :institutionId AND u.currentStreak > 0 AND (u.lastActiveDate IS NULL OR u.lastActiveDate < :threshold)")
    List<UserJpa> findAllInactiveSince(@Param("threshold") LocalDate threshold, @Param("institutionId") Long institutionId);

    @Query("SELECT u FROM UserJpa u WHERE u.institutionId = :institutionId " +
            "AND u.role = com.streakstudy.domain.model.UserRole.STUDENT " +
            "ORDER BY u.xp DESC")
    List<UserJpa> findTopUsersByXp(@Param("institutionId") Long institutionId, Limit limit);

    @Query("SELECT u FROM UserJpa u WHERE u.institutionId = :institutionId " +
            "AND u.role = com.streakstudy.domain.model.UserRole.STUDENT " +
            "ORDER BY u.xp DESC")
    List<UserJpa> findLeaderboardByInstitutionId(@Param("institutionId") Long institutionId);
}
