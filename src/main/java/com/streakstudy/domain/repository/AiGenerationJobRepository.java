package com.streakstudy.domain.repository;

import com.streakstudy.domain.model.AiGenerationJob;
import com.streakstudy.domain.model.AiGenerationJobStatus;

import java.util.Optional;

public interface AiGenerationJobRepository {
    AiGenerationJob save(AiGenerationJob job);
    Optional<AiGenerationJob> findById(Long id);
    Optional<AiGenerationJob> findByDocumentIdAndStatus(Long documentId, AiGenerationJobStatus status);
}
