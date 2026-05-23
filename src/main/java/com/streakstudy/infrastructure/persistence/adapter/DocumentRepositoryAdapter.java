package com.streakstudy.infrastructure.persistence.adapter;

import com.streakstudy.domain.model.Document;
import com.streakstudy.domain.repository.DocumentRepository;
import com.streakstudy.infrastructure.persistence.mapper.DocumentMapper;
import com.streakstudy.infrastructure.persistence.repository.DocumentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpa;

    public DocumentRepositoryAdapter(DocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Document save(Document document) {
        return DocumentMapper.toDomain(jpa.save(DocumentMapper.toJpa(document)));
    }

    @Override
    public Optional<Document> findById(Long id) {
        return jpa.findById(id).map(DocumentMapper::toDomain);
    }

    @Override
    public Optional<Document> findByFileHash(String fileHash) {
        return jpa.findByFileHash(fileHash).map(DocumentMapper::toDomain);
    }
}
