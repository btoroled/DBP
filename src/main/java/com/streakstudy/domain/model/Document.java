package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Document implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final Long uploadedBy;
    private final String originalFilename;
    private final long sizeBytes;
    private final String fileHash;
    private final DocumentStatus status;
    private final String markdownContent;
    private final Instant createdAt;

    public Document(Long id, Long institutionId, Long uploadedBy, String originalFilename,
                    long sizeBytes, String fileHash, DocumentStatus status,
                    String markdownContent, Instant createdAt) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "uploadedBy");
        this.originalFilename = Objects.requireNonNull(originalFilename, "originalFilename");
        this.sizeBytes = sizeBytes;
        this.fileHash = fileHash;
        this.status = Objects.requireNonNull(status, "status");
        this.markdownContent = markdownContent;
        this.createdAt = createdAt;
    }

    public static Document newUpload(Long institutionId, Long uploadedBy,
                                     String originalFilename, long sizeBytes, String fileHash) {
        return new Document(null, institutionId, uploadedBy, originalFilename,
                sizeBytes, fileHash, DocumentStatus.PENDING, null, null);
    }

    public Document withMarkdown(String markdown) {
        return new Document(id, institutionId, uploadedBy, originalFilename,
                sizeBytes, fileHash, DocumentStatus.READY, markdown, createdAt);
    }

    public Document withStatus(DocumentStatus status) {
        return new Document(id, institutionId, uploadedBy, originalFilename,
                sizeBytes, fileHash, status, markdownContent, createdAt);
    }

    public Long id() { return id; }
    @Override public Long institutionId() { return institutionId; }
    public Long uploadedBy() { return uploadedBy; }
    public String originalFilename() { return originalFilename; }
    public long sizeBytes() { return sizeBytes; }
    public String fileHash() { return fileHash; }
    public DocumentStatus status() { return status; }
    public String markdownContent() { return markdownContent; }
    public Instant createdAt() { return createdAt; }
}
