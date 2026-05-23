package com.streakstudy.domain.repository;

import java.util.List;
import java.util.Optional;

import com.streakstudy.domain.model.Deck;

public interface DeckRepository {

    Deck save(Deck deck);

    Optional<Deck> findByIdAndInstitutionId(
            Long id,
            Long institutionId
    );

    List<Deck> findAllByInstitutionId(Long institutionId);

    long countByInstitutionId(Long institutionId);

    void deleteByIdAndInstitutionId(
            Long id,
            Long institutionId
    );
}