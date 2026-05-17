package com.streakstudy.application.dto;

import java.time.Instant;

import com.streakstudy.domain.model.Course;

public record CourseResponse(
    Long id,
    Long institutionId,
    String name,
    String description,
    Instant createdAt
) {
    public static CourseResponse from(Course c) {
        return new CourseResponse(c.id(), c.institutionId(), c.name(), c.description(), c.createdAt());
    }
}
