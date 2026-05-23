package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.AiGenerationJob;
import com.streakstudy.infrastructure.persistence.entity.AiGenerationJobJpa;

public final class AiGenerationJobMapper {

    private AiGenerationJobMapper() {}

    public static AiGenerationJob toDomain(AiGenerationJobJpa e) {
        return new AiGenerationJob(
                e.getId(), e.getDocumentId(), e.getDeckId(),
                e.getStatus(), e.getProvider(), e.getModel(),
                e.getTotalInputTokens(), e.getTotalOutputTokens(),
                e.getEstimatedCostUsd(), e.getErrorMessage(),
                e.getCreatedAt(), e.getCompletedAt()
        );
    }

    public static AiGenerationJobJpa toJpa(AiGenerationJob d) {
        AiGenerationJobJpa e = new AiGenerationJobJpa();
        e.setId(d.id());
        e.setDocumentId(d.documentId());
        e.setDeckId(d.deckId());
        e.setStatus(d.status());
        e.setProvider(d.provider());
        e.setModel(d.model());
        e.setTotalInputTokens(d.totalInputTokens());
        e.setTotalOutputTokens(d.totalOutputTokens());
        e.setEstimatedCostUsd(d.estimatedCostUsd());
        e.setErrorMessage(d.errorMessage());
        if (d.createdAt() != null) e.setCreatedAt(d.createdAt());
        e.setCompletedAt(d.completedAt());
        return e;
    }
}
