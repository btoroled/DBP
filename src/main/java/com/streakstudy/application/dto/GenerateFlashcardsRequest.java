package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerateFlashcardsRequest(
        @NotNull(message = "deckId es obligatorio")
        @Positive(message = "deckId debe ser positivo")
        Long deckId) {
}
