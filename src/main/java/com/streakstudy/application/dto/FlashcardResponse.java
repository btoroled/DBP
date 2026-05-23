package com.streakstudy.application.dto;

import java.time.Instant;

import com.streakstudy.domain.model.Flashcard;

public record FlashcardResponse(
        Long id,
        Long deckId,
        String question,
        String answer,
        Instant createdAt
) {

    public static FlashcardResponse from(Flashcard flashcard) {
        return new FlashcardResponse(
                flashcard.id(),
                flashcard.deckId(),
                flashcard.question(),
                flashcard.answer(),
                flashcard.createdAt()
        );
    }
}