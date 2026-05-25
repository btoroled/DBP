package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Flashcard implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final Long deckId;
    private String question;
    private String answer;
    private Difficulty difficulty;
    private final Instant createdAt;

    public Flashcard(
            Long id,
            Long institutionId,
            Long deckId,
            String question,
            String answer,
            Difficulty difficulty,
            Instant createdAt
    ) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId);
        this.deckId = Objects.requireNonNull(deckId);
        this.question = Objects.requireNonNull(question);
        this.answer = Objects.requireNonNull(answer);
        this.difficulty = Objects.requireNonNull(difficulty);
        this.createdAt = createdAt;
    }

    public static Flashcard newInstance(
            Long institutionId,
            Long deckId,
            String question,
            String answer,
            Difficulty difficulty
    ) {
        return new Flashcard(
                null,
                institutionId,
                deckId,
                question,
                answer,
                difficulty,
                null
        );
    }

    public void update(
            String question,
            String answer,
            Difficulty difficulty
    ) {
        this.question = Objects.requireNonNull(question);
        this.answer = Objects.requireNonNull(answer);
        this.difficulty = Objects.requireNonNull(difficulty);
    }

    public Long id() {
        return id;
    }

    @Override
    public Long institutionId() {
        return institutionId;
    }

    public Long deckId() {
        return deckId;
    }

    public String question() {
        return question;
    }

    public String answer() {
        return answer;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public Instant createdAt() {
        return createdAt;
    }
}