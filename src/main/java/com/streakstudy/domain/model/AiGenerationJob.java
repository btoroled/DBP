package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class AiGenerationJob {

    private static final double INPUT_COST_PER_TOKEN  = 0.80  / 1_000_000.0;
    private static final double OUTPUT_COST_PER_TOKEN = 4.00  / 1_000_000.0;

    private final Long   id;
    private final Long   documentId;
    private final Long   deckId;
    private final AiGenerationJobStatus status;
    private final String provider;
    private final String model;
    private final int    totalInputTokens;
    private final int    totalOutputTokens;
    private final double estimatedCostUsd;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant completedAt;

    public AiGenerationJob(Long id, Long documentId, Long deckId,
                           AiGenerationJobStatus status, String provider, String model,
                           int totalInputTokens, int totalOutputTokens,
                           double estimatedCostUsd, String errorMessage,
                           Instant createdAt, Instant completedAt) {
        this.id = id;
        this.documentId = Objects.requireNonNull(documentId);
        this.deckId = Objects.requireNonNull(deckId);
        this.status = Objects.requireNonNull(status);
        this.provider = provider;
        this.model = model;
        this.totalInputTokens = totalInputTokens;
        this.totalOutputTokens = totalOutputTokens;
        this.estimatedCostUsd = estimatedCostUsd;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static AiGenerationJob create(Long documentId, Long deckId, String provider, String model) {
        return new AiGenerationJob(null, documentId, deckId,
                AiGenerationJobStatus.PENDING, provider, model,
                0, 0, 0.0, null, null, null);
    }

    public AiGenerationJob withRunning() {
        return new AiGenerationJob(id, documentId, deckId,
                AiGenerationJobStatus.RUNNING, provider, model,
                totalInputTokens, totalOutputTokens, estimatedCostUsd,
                errorMessage, createdAt, null);
    }

    public AiGenerationJob withCompleted(int inputTokens, int outputTokens) {
        double cost = (inputTokens * INPUT_COST_PER_TOKEN) + (outputTokens * OUTPUT_COST_PER_TOKEN);
        return new AiGenerationJob(id, documentId, deckId,
                AiGenerationJobStatus.COMPLETED, provider, model,
                inputTokens, outputTokens, cost,
                null, createdAt, Instant.now());
    }

    public AiGenerationJob withFailed(String error) {
        return new AiGenerationJob(id, documentId, deckId,
                AiGenerationJobStatus.FAILED, provider, model,
                totalInputTokens, totalOutputTokens, estimatedCostUsd,
                error, createdAt, Instant.now());
    }

    public Long   id()                { return id; }
    public Long   documentId()        { return documentId; }
    public Long   deckId()            { return deckId; }
    public AiGenerationJobStatus status() { return status; }
    public String provider()          { return provider; }
    public String model()             { return model; }
    public int    totalInputTokens()  { return totalInputTokens; }
    public int    totalOutputTokens() { return totalOutputTokens; }
    public double estimatedCostUsd()  { return estimatedCostUsd; }
    public String errorMessage()      { return errorMessage; }
    public Instant createdAt()        { return createdAt; }
    public Instant completedAt()      { return completedAt; }
}
