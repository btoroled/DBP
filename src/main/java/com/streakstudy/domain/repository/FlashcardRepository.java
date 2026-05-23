package com.streakstudy.domain.repository;

import java.util.List;
import java.util.Optional;

import com.streakstudy.domain.model.Flashcard;

public interface FlashcardRepository {

    Flashcard save(Flashcard flashcard);

    Optional<Flashcard> findByIdAndInstitutionId(
            Long id,
            Long institutionId
    );

    List<Flashcard> findAllByDeckIdAndInstitutionId(
            Long deckId,
            Long institutionId
    );

    void deleteByIdAndInstitutionId(
            Long id,
            Long institutionId
    );
}