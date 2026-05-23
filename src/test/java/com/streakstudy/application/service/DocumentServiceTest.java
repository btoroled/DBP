package com.streakstudy.application.service;

import com.streakstudy.application.dto.AiGenerationJobResponse;
import com.streakstudy.application.dto.DocumentUploadResponse;
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
}
