package com.streakstudy.domain.repository;

import java.util.Optional;

import com.streakstudy.domain.model.Institution;

/**
 * Puerto de persistencia para Institution. Operaciones cross-tenant: la propia
 * Institution es la unidad de tenancy, asi que ninguna de estas operaciones
 * aplica el filtro tenant.
 */
public interface InstitutionRepository {

    Institution save(Institution institution);

    Optional<Institution> findById(Long id);

    Optional<Institution> findByCode(String code);

    boolean existsByCode(String code);
}
