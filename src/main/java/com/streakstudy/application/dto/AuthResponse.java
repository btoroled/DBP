package com.streakstudy.application.dto;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;

public record AuthResponse(
    String token,
    long expiresInSeconds,
    Long userId,
    Long institutionId,
    String email,
    UserRole role
) {
    public static AuthResponse of(String token, long expiresIn, User u) {
        return new AuthResponse(token, expiresIn, u.id(), u.institutionId(), u.email(), u.role());
    }
}
