package com.streakstudy.infrastructure.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.streakstudy.infrastructure.persistence.entity.RewardItemJpa;

public interface RewardItemJpaRepository extends JpaRepository<RewardItemJpa, Long> {

    // Filtra la lista de la tienda para que cada colegio vea solo lo suyo
    List<RewardItemJpa> findByInstitutionId(Long institutionId);
}