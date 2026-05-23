package com.streakstudy.infrastructure.persistence.repository;

import com.streakstudy.infrastructure.persistence.entity.DocumentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentJpaRepository extends JpaRepository<DocumentJpa, Long> {
    Optional<DocumentJpa> findByFileHash(String fileHash);
}
