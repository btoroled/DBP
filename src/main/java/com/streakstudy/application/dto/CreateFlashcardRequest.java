package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFlashcardRequest(

        @NotNull
        Long deckId,

        @NotBlank
        String question,

        @NotBlank
        String answer

) {}