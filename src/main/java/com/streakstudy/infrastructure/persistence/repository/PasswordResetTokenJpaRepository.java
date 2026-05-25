package com.streakstudy.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.streakstudy.infrastructure.persistence.entity.PasswordResetTokenJpa;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpa, Long> {

    Optional<PasswordResetTokenJpa> findByTokenHash(String tokenHash);

    List<PasswordResetTokenJpa> findAllByUserIdAndUsedAtIsNullAndExpiresAtAfter(Long userId, Instant now);

    /**
     * Marca todos los tokens activos del usuario como usados. Idempotente.
     * Usado al emitir un token nuevo (rotacion: invalida los previos).
     */
    @Modifying
    @Query("update PasswordResetTokenJpa t set t.usedAt = :now " +
           "where t.userId = :userId and t.usedAt is null and t.expiresAt > :now")
    int invalidateAllActiveFor(@Param("userId") Long userId, @Param("now") Instant now);
}
