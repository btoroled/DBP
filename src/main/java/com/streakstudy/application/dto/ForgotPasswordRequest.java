package com.streakstudy.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "email es obligatorio")
    @Email(message = "email debe ser valido")
    String email
) { }
