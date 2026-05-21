package com.streakstudy.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streakstudy.infrastructure.persistence.entity.UserJpa;

public interface UserJpaRepository extends JpaRepository<UserJpa, Long> {
    /** Lookup cross-tenant intencional (solo para login). */
    Optional<UserJpa> findByEmail(String email);

    /** Lookup tenant-aware: garantiza que el id pertenece al tenant declarado. */
    Optional<UserJpa> findByIdAndInstitutionId(Long id, Long institutionId);

    boolean existsByEmail(String email);

    // NUEVA QUERY: FILTRA POR INSTITUCIÓN Y ORDENA POR RACHA DESCENDENTE
    List<UserJpa> findByInstitutionIdOrderByStreakDesc(Long institutionId);
}