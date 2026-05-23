package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description

) {
}