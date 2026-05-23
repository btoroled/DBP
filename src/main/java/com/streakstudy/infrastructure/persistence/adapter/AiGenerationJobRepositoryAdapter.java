package com.streakstudy.infrastructure.persistence.adapter;

import com.streakstudy.domain.model.AiGenerationJob;
import com.streakstudy.domain.model.AiGenerationJobStatus;
import com.streakstudy.domain.repository.AiGenerationJobRepository;
import com.streakstudy.infrastructure.persistence.mapper.AiGenerationJobMapper;
import com.streakstudy.infrastructure.persistence.repository.AiGenerationJobJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AiGenerationJobRepositoryAdapter implements AiGenerationJobRepository {

    private final AiGenerationJobJpaRepository jpa;

    public AiGenerationJobRepositoryAdapter(AiGenerationJobJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AiGenerationJob save(AiGenerationJob job) {
        return AiGenerationJobMapper.toDomain(jpa.save(AiGenerationJobMapper.toJpa(job)));
    }

    @Override
    public Optional<AiGenerationJob> findById(Long id) {
        return jpa.findById(id).map(AiGenerationJobMapper::toDomain);
    }

    @Override
    public Optional<AiGenerationJob> findByDocumentIdAndStatus(Long documentId, AiGenerationJobStatus status) {
        return jpa.findByDocumentIdAndStatus(documentId, status).map(AiGenerationJobMapper::toDomain);
    }
}
