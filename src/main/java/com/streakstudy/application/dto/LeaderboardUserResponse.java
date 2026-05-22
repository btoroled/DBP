package com.streakstudy.application.dto;

public record LeaderboardUserResponse(
        Long id,
        String fullName,
        Integer streak,
        Integer points
) {}