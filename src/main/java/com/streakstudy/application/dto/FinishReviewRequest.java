package com.streakstudy.application.dto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
public record FinishReviewRequest(
        @Positive int reviewedCards,
        @PositiveOrZero int durationMinutes
) {}