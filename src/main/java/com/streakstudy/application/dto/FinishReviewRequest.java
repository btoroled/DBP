package com.streakstudy.application.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record FinishReviewRequest(
        @PositiveOrZero(message = "La XP ganada no puede ser negativa")
        int xpGained
) {
}
