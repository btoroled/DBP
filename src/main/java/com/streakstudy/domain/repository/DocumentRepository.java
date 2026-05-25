package com.streakstudy.domain.repository;

import com.streakstudy.domain.model.Document;

import java.util.Optional;

public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(Long id);
    Optional<Document> findByFileHash(String fileHash);
}
