package com.streakstudy.application.dto;
import com.streakstudy.domain.model.Badge;
import jakarta.validation.constraints.NotNull;
public record BadgePurchaseRequest(
        @NotNull Badge badgeName
) {}
