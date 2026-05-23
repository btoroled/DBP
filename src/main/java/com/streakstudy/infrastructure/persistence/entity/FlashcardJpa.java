package com.streakstudy.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(
        name = "flashcards",
        indexes = {
                @Index(name = "ix_flashcards_institution", columnList = "institution_id"),
                @Index(name = "ix_flashcards_deck", columnList = "deck_id")
        }
)
public class FlashcardJpa extends TenantAwareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, length = 2000)
    private String answer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}