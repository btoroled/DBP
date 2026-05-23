package com.streakstudy.application.dto;

import com.streakstudy.domain.model.DocumentStatus;

public record DocumentStatusResponse(
        Long documentId,
        String originalFilename,
        DocumentStatus status,
        boolean markdownAvailable
) {}
