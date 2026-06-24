package com.streakstudy.application.dto;

import com.streakstudy.domain.model.Institution;

public record InstitutionSummaryResponse(
    Long id,
    String name
) {
    public static InstitutionSummaryResponse from(Institution i) {
        return new InstitutionSummaryResponse(i.id(), i.name());
    }
}
