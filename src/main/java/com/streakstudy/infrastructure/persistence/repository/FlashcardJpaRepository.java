package com.streakstudy.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.streakstudy.infrastructure.persistence.entity.FlashcardJpa;

public interface FlashcardJpaRepository
        extends JpaRepository<FlashcardJpa, Long> {

    Optional<FlashcardJpa> findByIdAndInstitutionId(
            Long id,
            Long institutionId
    );

    List<FlashcardJpa> findAllByDeckIdAndInstitutionId(
            Long deckId,
            Long institutionId
    );

    @Modifying
    @Query("""
        delete from FlashcardJpa f
        where f.id = :id
        and f.institutionId = :institutionId
    """)
    int deleteByIdAndInstitutionIdScoped(
            Long id,
            Long institutionId
    );
}