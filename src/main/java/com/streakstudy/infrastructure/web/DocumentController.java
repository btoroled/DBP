package com.streakstudy.infrastructure.web;

import com.streakstudy.application.dto.AiGenerationJobResponse;
import com.streakstudy.application.dto.DocumentStatusResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
import com.streakstudy.application.dto.GenerateFlashcardsRequest;
import com.streakstudy.application.service.DocumentService;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.infrastructure.security.AuthenticatedUserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@PreAuthorize("isAuthenticated()")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Sube un PDF. Retorna 202 Accepted — el procesamiento ocurre en background. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.accepted().body(documentService.upload(file, principal.userId()));
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

    /** Dispara la generación de flashcards. Retorna 202 Accepted con el jobId para consultar estado. */
    @PostMapping("/{documentId}/generate-flashcards")
    public ResponseEntity<AiGenerationJobResponse> generateFlashcards(
            @PathVariable Long documentId,
            @Valid @RequestBody GenerateFlashcardsRequest request) {
        return ResponseEntity.accepted().body(documentService.triggerGeneration(documentId, request.deckId()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<AiGenerationJobResponse> getJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(documentService.getJobStatus(jobId));
    }

    @GetMapping("/{documentId}/flashcards")
    public ResponseEntity<List<Flashcard>> getFlashcards(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.getFlashcards(documentId));
    }
}
