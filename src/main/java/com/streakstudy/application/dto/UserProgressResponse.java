package com.streakstudy.application.dto;

import java.util.Set;

public record UserProgressResponse(
        int xp,
        int currentStreak,
        int streakFreezes,
        Set<String> badges
) {
}
