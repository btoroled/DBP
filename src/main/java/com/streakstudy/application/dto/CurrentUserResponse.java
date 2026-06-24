package com.streakstudy.application.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.streakstudy.domain.model.User;

public record CurrentUserResponse(
    Long userId,
    Long institutionId,
    String email,
    String role,
    int xp,
    int currentStreak,
    int streakFreezes,
    Set<String> badges
) {
    public static CurrentUserResponse from(User user) {
        Set<String> badgeNames = user.badges().stream()
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
        return new CurrentUserResponse(
            user.id(),
            user.institutionId(),
            user.email(),
            user.role().name(),
            user.xp(),
            user.currentStreak(),
            user.streakFreezes(),
            badgeNames
        );
    }
}
