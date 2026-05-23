package com.streakstudy.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.streakstudy.infrastructure.persistence.entity.DeckJpa;

public interface DeckJpaRepository
        extends JpaRepository<DeckJpa, Long> {

    Optional<DeckJpa> findByIdAndInstitutionId(
            Long id,
            Long institutionId
    );

    List<DeckJpa> findAllByInstitutionId(Long institutionId);

    long countByInstitutionId(Long institutionId);

    @Modifying
    @Query("""
        delete from DeckJpa d
        where d.id = :id
        and d.institutionId = :institutionId
    """)
    int deleteByIdAndInstitutionIdScoped(
            Long id,
            Long institutionId
    );
}