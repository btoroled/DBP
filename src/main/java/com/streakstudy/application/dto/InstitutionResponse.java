package com.streakstudy.application.dto;

import java.time.Instant;

import com.streakstudy.domain.model.Institution;

public record InstitutionResponse(
    Long id,
    String name,
    String code,
    boolean active,
    Instant createdAt
) {
    public static InstitutionResponse from(Institution i) {
        return new InstitutionResponse(i.id(), i.name(), i.code(), i.active(), i.createdAt());
    }
}
