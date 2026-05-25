package com.streakstudy.application.service;

import com.streakstudy.application.dto.AiGenerationJobResponse;
import com.streakstudy.application.dto.DocumentStatusResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
import com.streakstudy.domain.exception.EntityNotFoundException;
import com.streakstudy.domain.model.*;
import com.streakstudy.domain.repository.AiGenerationJobRepository;
import com.streakstudy.domain.repository.DocumentRepository;
import com.streakstudy.application.port.DocumentProcessingPort;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.infrastructure.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository        documentRepository;
    @Mock AiGenerationJobRepository jobRepository;
    @Mock FlashcardRepository       flashcardRepository;
    @Mock DocumentProcessingPort processingService;

    @InjectMocks DocumentService documentService;

    @BeforeEach void setTenant() { TenantContext.set(1L); }
    @AfterEach  void clearTenant() { TenantContext.clear(); }

    @Test
    void upload_archivoValido_guardaPendingYDisparaAsync() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "apuntes.pdf", "application/pdf", new byte[100]);

        Document saved = new Document(42L, 1L, 7L, "apuntes.pdf", 100, "abc123",
                DocumentStatus.PENDING, null, null);

        when(documentRepository.findByFileHash(any())).thenReturn(Optional.empty());
        when(documentRepository.save(any())).thenReturn(saved);

        DocumentUploadResponse response = documentService.upload(file, 7L);

        assertThat(response.documentId()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(response.duplicate()).isFalse();

        verify(processingService).processPdf(eq(42L), any(byte[].class), eq(1L));
    }

    @Test
    void upload_pdfDuplicado_retornaDuplicateTrue() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "dup.pdf", "application/pdf", new byte[50]);

        Document existente = new Document(10L, 1L, 5L, "dup.pdf", 50, "hash",
                DocumentStatus.READY, "texto", null);

        when(documentRepository.findByFileHash(any())).thenReturn(Optional.of(existente));

        DocumentUploadResponse response = documentService.upload(file, 5L);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.documentId()).isEqualTo(10L);
        verify(processingService, never()).processPdf(any(), any(), any());
    }

    @Test
    void upload_noEsPdf_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[100]);

        assertThatThrownBy(() -> documentService.upload(file, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void triggerGeneration_documentoListo_creaJobYDisparaAsync() {
        Document doc = new Document(1L, 1L, 5L, "apuntes.pdf", 100, "hash",
                DocumentStatus.READY, "Texto de prueba\n\nSegundo párrafo.", null);

        AiGenerationJob savedJob = new AiGenerationJob(77L, 1L, 99L,
                AiGenerationJobStatus.PENDING, "anthropic", "claude-haiku-4-5-20251001",
                0, 0, 0.0, null, null, null);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(jobRepository.findByDocumentIdAndStatus(1L, AiGenerationJobStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(jobRepository.save(any())).thenReturn(savedJob);

        AiGenerationJobResponse response = documentService.triggerGeneration(1L, 99L);

        assertThat(response.status()).isEqualTo(AiGenerationJobStatus.PENDING);
        verify(processingService).generateFlashcards(eq(77L), any(), eq(99L), eq(1L));
    }

    @Test
    void triggerGeneration_yaExisteJobCompletado_lanzaExcepcion() {
        Document doc = new Document(1L, 1L, 5L, "a.pdf", 10, "h",
                DocumentStatus.READY, "texto", null);
        AiGenerationJob completado = new AiGenerationJob(5L, 1L, 2L,
                AiGenerationJobStatus.COMPLETED, "anthropic", "model",
                100, 200, 0.001, null, null, null);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(jobRepository.findByDocumentIdAndStatus(1L, AiGenerationJobStatus.COMPLETED))
                .thenReturn(Optional.of(completado));

        assertThatThrownBy(() -> documentService.triggerGeneration(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fueron generadas");
    }

    @Test
    void shouldThrowWhenUploadingEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentService.upload(empty, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void shouldThrowWhenUploadingFileBeyondMaxSize() {
        // size limit is 20 MB; we provide content of length 21 MB
        byte[] huge = new byte[21 * 1024 * 1024];
        MockMultipartFile big = new MockMultipartFile(
                "file", "huge.pdf", "application/pdf", huge);

        assertThatThrownBy(() -> documentService.upload(big, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 MB");
    }

    @Test
    void shouldThrowWhenUploadingFileWithNullContentType() {
        MockMultipartFile nullCt = new MockMultipartFile(
                "file", "x.pdf", null, new byte[10]);

        assertThatThrownBy(() -> documentService.upload(nullCt, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void shouldReturnDocumentStatusWhenFound() {
        Document doc = new Document(3L, 1L, 5L, "n.pdf", 10, "h",
                DocumentStatus.READY, "markdown body", null);
        when(documentRepository.findById(3L)).thenReturn(Optional.of(doc));

        DocumentStatusResponse resp = documentService.getStatus(3L);

        assertThat(resp.documentId()).isEqualTo(3L);
        assertThat(resp.originalFilename()).isEqualTo("n.pdf");
        assertThat(resp.status()).isEqualTo(DocumentStatus.READY);
        assertThat(resp.markdownAvailable()).isTrue();
    }

    @Test
    void shouldReportNoMarkdownInStatusWhenContentIsNull() {
        Document doc = new Document(4L, 1L, 5L, "p.pdf", 10, "h",
                DocumentStatus.PENDING, null, null);
        when(documentRepository.findById(4L)).thenReturn(Optional.of(doc));

        DocumentStatusResponse resp = documentService.getStatus(4L);

        assertThat(resp.markdownAvailable()).isFalse();
    }

    @Test
    void shouldThrowEntityNotFoundWhenGettingStatusOfMissingDocument() {
        when(documentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getStatus(404L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldReturnMarkdownWhenDocumentHasContent() {
        Document doc = new Document(7L, 1L, 5L, "d.pdf", 10, "h",
                DocumentStatus.READY, "# Heading", null);
        when(documentRepository.findById(7L)).thenReturn(Optional.of(doc));

        String md = documentService.getMarkdown(7L);

        assertThat(md).isEqualTo("# Heading");
    }

    @Test
    void shouldThrowWhenGettingMarkdownThatIsNotReady() {
        Document doc = new Document(8L, 1L, 5L, "d.pdf", 10, "h",
                DocumentStatus.PENDING, null, null);
        when(documentRepository.findById(8L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.getMarkdown(8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aún no está disponible");
    }

    @Test
    void shouldThrowEntityNotFoundWhenGettingMarkdownOfMissingDocument() {
        when(documentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getMarkdown(404L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowEntityNotFoundWhenTriggeringGenerationForMissingDocument() {
        when(documentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.triggerGeneration(404L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowWhenTriggeringGenerationOnDocumentNotReady() {
        Document doc = new Document(9L, 1L, 5L, "p.pdf", 10, "h",
                DocumentStatus.PROCESSING, null, null);
        when(documentRepository.findById(9L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.triggerGeneration(9L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está listo");
    }

    @Test
    void shouldReturnJobStatusWhenJobExists() {
        AiGenerationJob job = new AiGenerationJob(50L, 11L, 22L,
                AiGenerationJobStatus.RUNNING, "anthropic", "model",
                100, 200, 0.002, null, null, null);
        when(jobRepository.findById(50L)).thenReturn(Optional.of(job));

        AiGenerationJobResponse resp = documentService.getJobStatus(50L);

        assertThat(resp.jobId()).isEqualTo(50L);
        assertThat(resp.documentId()).isEqualTo(11L);
        assertThat(resp.deckId()).isEqualTo(22L);
        assertThat(resp.status()).isEqualTo(AiGenerationJobStatus.RUNNING);
        assertThat(resp.totalInputTokens()).isEqualTo(100);
        assertThat(resp.totalOutputTokens()).isEqualTo(200);
        assertThat(resp.estimatedCostUsd()).isEqualTo(0.002);
    }

    @Test
    void shouldThrowEntityNotFoundWhenGettingMissingJobStatus() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getJobStatus(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldReturnFlashcardsWhenCompletedJobExistsForDocument() {
        Document doc = new Document(20L, 1L, 5L, "x.pdf", 10, "h",
                DocumentStatus.READY, "txt", null);
        AiGenerationJob job = new AiGenerationJob(60L, 20L, 33L,
                AiGenerationJobStatus.COMPLETED, "anthropic", "model",
                10, 20, 0.001, null, null, null);
        Flashcard fc = Flashcard.newInstance(1L, 33L, "Q", "A", Difficulty.EASY);

        when(documentRepository.findById(20L)).thenReturn(Optional.of(doc));
        when(jobRepository.findByDocumentIdAndStatus(20L, AiGenerationJobStatus.COMPLETED))
                .thenReturn(Optional.of(job));
        when(flashcardRepository.findAllByDeckIdAndInstitutionId(33L, 1L))
                .thenReturn(List.of(fc));

        List<Flashcard> result = documentService.getFlashcards(20L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deckId()).isEqualTo(33L);
    }

    @Test
    void shouldReturnEmptyFlashcardListWhenNoCompletedJobForDocument() {
        Document doc = new Document(21L, 1L, 5L, "x.pdf", 10, "h",
                DocumentStatus.READY, "txt", null);
        when(documentRepository.findById(21L)).thenReturn(Optional.of(doc));
        when(jobRepository.findByDocumentIdAndStatus(21L, AiGenerationJobStatus.COMPLETED))
                .thenReturn(Optional.empty());

        List<Flashcard> result = documentService.getFlashcards(21L);

        assertThat(result).isEmpty();
        verify(flashcardRepository, never()).findAllByDeckIdAndInstitutionId(any(), any());
    }

    @Test
    void shouldThrowEntityNotFoundWhenGettingFlashcardsForMissingDocument() {
        when(documentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getFlashcards(404L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
