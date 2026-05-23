package com.streakstudy.application.service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.streakstudy.application.dto.DocumentStatusResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
import com.streakstudy.application.port.AiFlashcardGeneratorPort;
import com.streakstudy.application.port.AiFlashcardGeneratorPort.FlashcardSuggestion;
import com.streakstudy.application.port.PdfTextExtractorPort;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.Document;
import com.streakstudy.domain.model.DocumentStatus;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.repository.DocumentRepository;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@Service
public class DocumentService {

    private static final int MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB
    private static final int CHUNK_SIZE = 2000;

    private final DocumentRepository documentRepository;
    private final FlashcardRepository flashcardRepository;
    private final PdfTextExtractorPort pdfExtractor;
    private final AiFlashcardGeneratorPort aiGenerator;

    public DocumentService(DocumentRepository documentRepository,
                           FlashcardRepository flashcardRepository,
                           PdfTextExtractorPort pdfExtractor,
                           AiFlashcardGeneratorPort aiGenerator) {
        this.documentRepository = documentRepository;
        this.flashcardRepository = flashcardRepository;
        this.pdfExtractor = pdfExtractor;
        this.aiGenerator = aiGenerator;
    }

    @Transactional
    public DocumentUploadResponse upload(MultipartFile file, Long userId) {
        validateFile(file);

        String hash = sha256(file);

        // Detect duplicate by hash within tenant
        var existing = documentRepository.findByFileHash(hash);
        if (existing.isPresent()) {
            Document doc = existing.get();
            return new DocumentUploadResponse(doc.id(), doc.originalFilename(), doc.status(), true);
        }

        Document doc = Document.newUpload(
                TenantContext.requireInstitutionId(),
                userId,
                file.getOriginalFilename(),
                file.getSize(),
                hash
        );
        doc = documentRepository.save(doc);

        // Extract text and update status synchronously
        try {
            doc = documentRepository.save(doc.withStatus(DocumentStatus.PROCESSING));
            String text = pdfExtractor.extract(file.getInputStream());
            doc = documentRepository.save(doc.withMarkdown(text));
        } catch (Exception e) {
            documentRepository.save(doc.withStatus(DocumentStatus.FAILED));
            throw new RuntimeException("Error al procesar el PDF: " + e.getMessage(), e);
        }

        return new DocumentUploadResponse(doc.id(), doc.originalFilename(), doc.status(), false);
    }

    @Transactional(readOnly = true)
    public DocumentStatusResponse getStatus(Long documentId) {
        Document doc = requireDocument(documentId);
        return new DocumentStatusResponse(
                doc.id(),
                doc.originalFilename(),
                doc.status(),
                doc.markdownContent() != null
        );
    }

    @Transactional(readOnly = true)
    public String getMarkdown(Long documentId) {
        Document doc = requireDocument(documentId);
        if (doc.markdownContent() == null) {
            throw new IllegalStateException("El markdown aún no está disponible para este documento");
        }
        return doc.markdownContent();
    }

    @Transactional
    public List<FlashcardSuggestion> generateFlashcards(Long documentId, Long deckId) {
        Document doc = requireDocument(documentId);
        if (doc.status() != DocumentStatus.READY) {
            throw new IllegalStateException("El documento no está listo. Estado actual: " + doc.status());
        }

        List<String> chunks = chunk(doc.markdownContent(), CHUNK_SIZE);

        List<FlashcardSuggestion> suggestions = chunks.stream()
                .flatMap(c -> aiGenerator.generate(c).stream())
                .toList();

        Long institutionId = TenantContext.requireInstitutionId();
        suggestions.forEach(s -> flashcardRepository.save(
                Flashcard.newInstance(institutionId, deckId, s.question(), s.answer())
        ));

        return suggestions;
    }

    private Document requireDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document", documentId));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (file.getSize() > MAX_FILE_SIZE_BYTES) throw new IllegalArgumentException("El archivo supera el límite de 20 MB");
        String ct = file.getContentType();
        if (ct == null || !ct.equals("application/pdf")) {
            throw new IllegalArgumentException("Solo se aceptan archivos PDF");
        }
    }

    private static String sha256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) digest.update(buf, 0, n);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular hash del archivo", e);
        }
    }

    static List<String> chunk(String text, int maxChars) {
        String[] paragraphs = text.split("\n\n+");
        List<String> chunks = new java.util.ArrayList<>();
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
