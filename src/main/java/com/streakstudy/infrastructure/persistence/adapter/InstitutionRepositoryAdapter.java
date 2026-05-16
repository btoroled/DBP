package com.streakstudy.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.streakstudy.domain.model.Institution;
import com.streakstudy.domain.repository.InstitutionRepository;
import com.streakstudy.infrastructure.persistence.entity.InstitutionJpa;
import com.streakstudy.infrastructure.persistence.mapper.InstitutionMapper;
import com.streakstudy.infrastructure.persistence.repository.InstitutionJpaRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Component
public class InstitutionRepositoryAdapter implements InstitutionRepository {

    private final InstitutionJpaRepository jpa;

    public InstitutionRepositoryAdapter(InstitutionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Institution save(Institution institution) {
        // Crear instituciones es siempre cross-tenant
        return TenantContext.runCrossTenant(() -> {
            InstitutionJpa saved = jpa.save(InstitutionMapper.toJpa(institution));
            return InstitutionMapper.toDomain(saved);
        });
    }

    @Override
    public Optional<Institution> findById(Long id) {
        return jpa.findById(id).map(InstitutionMapper::toDomain);
    }

    @Override
    public Optional<Institution> findByCode(String code) {
        return jpa.findByCode(code).map(InstitutionMapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
