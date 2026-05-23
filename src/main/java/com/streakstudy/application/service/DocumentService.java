package com.streakstudy.application.service;

import com.streakstudy.application.dto.AiGenerationJobResponse;
import com.streakstudy.application.dto.DocumentStatusResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.AiGenerationJob;
import com.streakstudy.domain.model.AiGenerationJobStatus;
import com.streakstudy.domain.model.Document;
import com.streakstudy.domain.model.DocumentStatus;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.repository.AiGenerationJobRepository;
import com.streakstudy.domain.repository.DocumentRepository;
import com.streakstudy.application.port.DocumentProcessingPort;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.infrastructure.ai.AnthropicFlashcardGeneratorAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class DocumentService {

    public static final int CHUNK_SIZE = 2000;

    private static final int MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024;

    private final DocumentRepository        documentRepository;
    private final AiGenerationJobRepository jobRepository;
    private final FlashcardRepository       flashcardRepository;
    private final DocumentProcessingPort processingService;

    public DocumentService(DocumentRepository documentRepository,
                           AiGenerationJobRepository jobRepository,
                           FlashcardRepository flashcardRepository,
                           DocumentProcessingPort processingService) {
        this.documentRepository  = documentRepository;
        this.jobRepository       = jobRepository;
        this.flashcardRepository = flashcardRepository;
        this.processingService   = processingService;
    }

    @Transactional
    public DocumentUploadResponse upload(MultipartFile file, Long userId) {
        validateFile(file);

        byte[] bytes = readBytes(file);
        String hash  = sha256(bytes);

        var existing = documentRepository.findByFileHash(hash);
        if (existing.isPresent()) {
            Document doc = existing.get();
            return new DocumentUploadResponse(doc.id(), doc.originalFilename(), doc.status(), true);
        }

        Long institutionId = TenantContext.requireInstitutionId();
        Document doc = documentRepository.save(
                Document.newUpload(institutionId, userId, file.getOriginalFilename(), file.getSize(), hash));

        // Procesamiento asíncrono: el hilo HTTP retorna de inmediato
        processingService.processPdf(doc.id(), bytes, institutionId);

        return new DocumentUploadResponse(doc.id(), doc.originalFilename(), DocumentStatus.PENDING, false);
    }

    @Transactional(readOnly = true)
    public DocumentStatusResponse getStatus(Long documentId) {
        Document doc = requireDocument(documentId);
        return new DocumentStatusResponse(doc.id(), doc.originalFilename(), doc.status(),
                doc.markdownContent() != null);
    }

    @Transactional(readOnly = true)
    public String getMarkdown(Long documentId) {
        Document doc = requireDocument(documentId);
        if (doc.markdownContent() == null)
            throw new IllegalStateException("El markdown aún no está disponible. Estado: " + doc.status());
        return doc.markdownContent();
    }

    @Transactional
    public AiGenerationJobResponse triggerGeneration(Long documentId, Long deckId) {
        Document doc = requireDocument(documentId);
        if (doc.status() != DocumentStatus.READY)
            throw new IllegalStateException("El documento no está listo. Estado: " + doc.status());

        // Evitar regeneración si ya existe un job completado
        if (jobRepository.findByDocumentIdAndStatus(documentId, AiGenerationJobStatus.COMPLETED).isPresent())
            throw new IllegalStateException("Las flashcards ya fueron generadas para este documento");

        AiGenerationJob job = jobRepository.save(AiGenerationJob.create(
                documentId, deckId, AnthropicFlashcardGeneratorAdapter.PROVIDER,
                AnthropicFlashcardGeneratorAdapter.MODEL));

        processingService.generateFlashcards(job.id(), doc.markdownContent(), deckId,
                TenantContext.requireInstitutionId());

        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public AiGenerationJobResponse getJobStatus(Long jobId) {
        return jobRepository.findById(jobId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("AiGenerationJob", jobId));
    }

    @Transactional(readOnly = true)
    public List<Flashcard> getFlashcards(Long documentId) {
        requireDocument(documentId);
        Long institutionId = TenantContext.requireInstitutionId();
        return jobRepository.findByDocumentIdAndStatus(documentId, AiGenerationJobStatus.COMPLETED)
                .map(job -> flashcardRepository.findAllByDeckIdAndInstitutionId(job.deckId(), institutionId))
                .orElse(List.of());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Document requireDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document", id));
    }

    private AiGenerationJobResponse toResponse(AiGenerationJob j) {
        return new AiGenerationJobResponse(j.id(), j.documentId(), j.deckId(), j.status(),
                j.totalInputTokens(), j.totalOutputTokens(), j.estimatedCostUsd(), j.errorMessage());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("El archivo supera el límite de 20 MB");
        String ct = file.getContentType();
        if (ct == null || !ct.equals("application/pdf"))
            throw new IllegalArgumentException("Solo se aceptan archivos PDF");
    }

    private static byte[] readBytes(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new RuntimeException("Error al leer el archivo", e); }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) { throw new RuntimeException("Error al calcular hash", e); }
    }

    public static List<String> chunk(String text, int maxChars) {
        String[] paragraphs = text.split("\n\n+");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            String p = para.strip();
            if (p.isBlank()) continue;
            if (current.length() + p.length() > maxChars && !current.isEmpty()) {
                chunks.add(current.toString().strip());
                current.setLength(0);
            }
            current.append(p).append("\n\n");
        }
        if (!current.isEmpty()) chunks.add(current.toString().strip());
        return chunks;
    }
}
