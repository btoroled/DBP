package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.entity.InstitutionJpa;

public final class InstitutionMapper {

    private InstitutionMapper() { }

    public static Institution toDomain(InstitutionJpa jpa) {
        return new Institution(jpa.getId(), jpa.getName(), jpa.getCode(), jpa.isActive(), jpa.getCreatedAt());
    }

    public static InstitutionJpa toJpa(Institution domain) {
        InstitutionJpa jpa = new InstitutionJpa();
        jpa.setId(domain.id());
        jpa.setName(domain.name());
        jpa.setCode(domain.code());
        jpa.setActive(domain.active());
        jpa.setCreatedAt(domain.createdAt());
        return jpa;
    }
}
