package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.Document;
import com.streakstudy.infrastructure.persistence.entity.DocumentJpa;

public final class DocumentMapper {

    private DocumentMapper() {}

    public static Document toDomain(DocumentJpa e) {
        return new Document(
                e.getId(),
                e.getInstitutionId(),
                e.getUploadedBy(),
                e.getOriginalFilename(),
                e.getSizeBytes(),
                e.getFileHash(),
                e.getStatus(),
                e.getMarkdownContent(),
                e.getCreatedAt()
        );
    }

    public static DocumentJpa toJpa(Document d) {
        DocumentJpa e = new DocumentJpa();
        e.setId(d.id());
        e.setInstitutionId(d.institutionId());
        e.setUploadedBy(d.uploadedBy());
        e.setOriginalFilename(d.originalFilename());
        e.setSizeBytes(d.sizeBytes());
        e.setFileHash(d.fileHash());
        e.setStatus(d.status());
        e.setMarkdownContent(d.markdownContent());
        if (d.createdAt() != null) e.setCreatedAt(d.createdAt());
        return e;
    }
}
