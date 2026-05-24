package com.streakstudy.application.dto;

import com.streakstudy.domain.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFlashcardRequest(

        @NotNull
        Long deckId,

        @NotBlank
        @Size(max = 500)
        String question,
        @NotBlank
        String answer,

        @NotNull
        Difficulty difficulty
) {}