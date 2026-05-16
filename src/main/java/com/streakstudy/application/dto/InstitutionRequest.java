package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstitutionRequest(
    @NotBlank @Size(min = 2, max = 120) String name,
    @NotBlank
    @Size(min = 2, max = 32)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "code debe contener solo minusculas, digitos o guiones")
    String code
) { }
