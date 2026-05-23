package com.streakstudy.infrastructure.persistence.entity;

import com.streakstudy.domain.model.AiGenerationJobStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "ai_generation_jobs",
        indexes = @Index(name = "ix_ai_jobs_document", columnList = "document_id")
)
public class AiGenerationJobJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiGenerationJobStatus status;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "total_input_tokens", nullable = false)
    private int totalInputTokens;

    @Column(name = "total_output_tokens", nullable = false)
    private int totalOutputTokens;

    @Column(name = "estimated_cost_usd", nullable = false)
    private double estimatedCostUsd;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getDeckId() { return deckId; }
    public void setDeckId(Long deckId) { this.deckId = deckId; }
    public AiGenerationJobStatus getStatus() { return status; }
    public void setStatus(AiGenerationJobStatus status) { this.status = status; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTotalInputTokens() { return totalInputTokens; }
    public void setTotalInputTokens(int v) { this.totalInputTokens = v; }
    public int getTotalOutputTokens() { return totalOutputTokens; }
    public void setTotalOutputTokens(int v) { this.totalOutputTokens = v; }
    public double getEstimatedCostUsd() { return estimatedCostUsd; }
    public void setEstimatedCostUsd(double v) { this.estimatedCostUsd = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
