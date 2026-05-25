package com.streakstudy.application.dto;

import com.streakstudy.domain.model.DocumentStatus;

public record DocumentUploadResponse(
        Long documentId,
        String originalFilename,
        DocumentStatus status,
        boolean duplicate
) {}
