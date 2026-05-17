package com.streakstudy.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streakstudy.infrastructure.persistence.entity.InstitutionJpa;

public interface InstitutionJpaRepository extends JpaRepository<InstitutionJpa, Long> {
    Optional<InstitutionJpa> findByCode(String code);
    boolean existsByCode(String code);
}
