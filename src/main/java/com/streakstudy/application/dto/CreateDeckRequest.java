package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeckRequest(

        @NotBlank(message = "name is required")
        String name,

        String description

) {}