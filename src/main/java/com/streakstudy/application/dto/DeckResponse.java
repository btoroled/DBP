package com.streakstudy.application.dto;

import java.time.Instant;

import com.streakstudy.domain.model.Deck;

public record DeckResponse(
        Long id,
        String name,
        String description,
        Instant createdAt
) {

    public static DeckResponse from(Deck deck) {
        return new DeckResponse(
                deck.id(),
                deck.name(),
                deck.description(),
                deck.createdAt()
        );
    }
}