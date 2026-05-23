package com.streakstudy.application.dto;

import com.streakstudy.domain.model.AiGenerationJobStatus;

public record AiGenerationJobResponse(
        Long jobId,
        Long documentId,
        Long deckId,
        AiGenerationJobStatus status,
        int totalInputTokens,
        int totalOutputTokens,
        double estimatedCostUsd,
        String errorMessage
) {}
