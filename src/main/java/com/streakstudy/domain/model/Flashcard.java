package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Flashcard implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final Long deckId;

    private String question;
    private String answer;

    private final Instant createdAt;

    public Flashcard(
            Long id,
            Long institutionId,
            Long deckId,
            String question,
            String answer,
            Instant createdAt
    ) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId);
        this.deckId = Objects.requireNonNull(deckId);
        this.question = Objects.requireNonNull(question);
        this.answer = Objects.requireNonNull(answer);
        this.createdAt = createdAt;
    }

    public static Flashcard newInstance(
            Long institutionId,
            Long deckId,
            String question,
            String answer
    ) {
        return new Flashcard(
                null,
                institutionId,
                deckId,
                question,
                answer,
                null
        );
    }

    public void update(String question, String answer) {
        this.question = Objects.requireNonNull(question);
        this.answer = Objects.requireNonNull(answer);
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

    public Instant createdAt() {
        return createdAt;
    }
}