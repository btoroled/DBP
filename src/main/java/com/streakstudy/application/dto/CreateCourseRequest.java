package com.streakstudy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
    @NotBlank @Size(min = 2, max = 200) String name,
    @Size(max = 2000) String description
) { }
