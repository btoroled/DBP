package com.streakstudy.infrastructure.persistence.repository;

import com.streakstudy.domain.model.AiGenerationJobStatus;
import com.streakstudy.infrastructure.persistence.entity.AiGenerationJobJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiGenerationJobJpaRepository extends JpaRepository<AiGenerationJobJpa, Long> {
    Optional<AiGenerationJobJpa> findByDocumentIdAndStatus(Long documentId, AiGenerationJobStatus status);
}
