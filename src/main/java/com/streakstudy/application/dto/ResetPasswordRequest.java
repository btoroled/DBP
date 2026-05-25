package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "token es obligatorio")
    String token,

    @NotBlank(message = "newPassword es obligatorio")
    @Size(min = 8, max = 100, message = "newPassword debe tener entre 8 y 100 caracteres")
    String newPassword
) { }
