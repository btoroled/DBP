package com.streakstudy.application.dto;

import com.streakstudy.domain.model.User;
import com.streakstudy.domain.model.UserRole;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    Long userId,
    Long institutionId,
    String email,
    UserRole role,
    int xp
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, User u) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, u.id(), u.institutionId(), u.email(), u.role(), u.xp());
    }
}
