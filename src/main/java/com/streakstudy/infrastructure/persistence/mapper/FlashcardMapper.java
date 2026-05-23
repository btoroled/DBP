package com.streakstudy.infrastructure.persistence.mapper;

import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.infrastructure.persistence.entity.FlashcardJpa;

public final class FlashcardMapper {

    private FlashcardMapper() {}

    public static Flashcard toDomain(FlashcardJpa jpa) {

        return new Flashcard(
                jpa.getId(),
                jpa.getInstitutionId(),
                jpa.getDeckId(),
                jpa.getQuestion(),
                jpa.getAnswer(),
                jpa.getCreatedAt()
        );
    }

    public static FlashcardJpa toJpa(Flashcard domain) {

        FlashcardJpa jpa = new FlashcardJpa();

        jpa.setId(domain.id());
        jpa.setInstitutionId(domain.institutionId());
        jpa.setDeckId(domain.deckId());
        jpa.setQuestion(domain.question());
        jpa.setAnswer(domain.answer());
        jpa.setCreatedAt(domain.createdAt());

        return jpa;
    }
}