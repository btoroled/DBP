package com.streakstudy.application.dto;

public record RewardItemResponse(
        Long id,
        String title,
        String description,
        Integer costInPoints,
        Integer stock
) {}