package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "El nombre debe tener <= 200 caracteres")
        String name,

        @Size(max = 2000, message = "El nombre debe tener <= 200 caracteres")
        String description

) { }