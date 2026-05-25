package com.streakstudy.application.dto;

import com.streakstudy.domain.model.Difficulty;
import com.streakstudy.domain.model.Flashcard;

import java.time.Instant;

public record FlashcardDetailResponse(
        Long id,
        Long deckId,
        String question,
        String answer,
        Instant createdAt,
        Difficulty difficulty
) {

    public static FlashcardDetailResponse from(
            Flashcard flashcard
    ) {
        return new FlashcardDetailResponse(
                flashcard.id(),
                flashcard.deckId(),
                flashcard.question(),
                flashcard.answer(),
                flashcard.createdAt(),
                flashcard.difficulty()
        );
    }
}