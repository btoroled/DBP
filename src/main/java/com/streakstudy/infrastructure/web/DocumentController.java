package com.streakstudy.infrastructure.web;

import com.streakstudy.application.dto.DocumentStatusResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
import com.streakstudy.application.dto.GenerateFlashcardsRequest;
import com.streakstudy.application.port.AiFlashcardGeneratorPort.FlashcardSuggestion;
import com.streakstudy.application.service.DocumentService;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("isAuthenticated()")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        DocumentUploadResponse response = documentService.upload(file, principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getStatus(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.getStatus(documentId));
    }

    @GetMapping("/{documentId}/markdown")
    public ResponseEntity<String> getMarkdown(@PathVariable Long documentId) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(documentService.getMarkdown(documentId));
    }

    @PostMapping("/{documentId}/generate-flashcards")
    public ResponseEntity<List<FlashcardSuggestion>> generateFlashcards(
            @PathVariable Long documentId,
            @Valid @RequestBody GenerateFlashcardsRequest request) {
        List<FlashcardSuggestion> result = documentService.generateFlashcards(documentId, request.deckId());
        return ResponseEntity.ok(result);
    }
}
