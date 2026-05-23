package com.streakstudy.application.service;

import com.streakstudy.application.port.AiFlashcardGeneratorPort;
import com.streakstudy.application.port.AiFlashcardGeneratorPort.GenerationResult;
import com.streakstudy.application.port.PdfTextExtractorPort;
import com.streakstudy.domain.model.DocumentStatus;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.repository.AiGenerationJobRepository;
import com.streakstudy.domain.repository.DocumentRepository;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.application.port.DocumentProcessingPort;
import com.streakstudy.infrastructure.tenancy.TenantContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
public class DocumentProcessingService implements DocumentProcessingPort {

    private final DocumentRepository      documentRepository;
    private final AiGenerationJobRepository jobRepository;
    private final FlashcardRepository     flashcardRepository;
    private final PdfTextExtractorPort    pdfExtractor;
    private final AiFlashcardGeneratorPort aiGenerator;

    public DocumentProcessingService(DocumentRepository documentRepository,
                                     AiGenerationJobRepository jobRepository,
                                     FlashcardRepository flashcardRepository,
                                     PdfTextExtractorPort pdfExtractor,
                                     AiFlashcardGeneratorPort aiGenerator) {
        this.documentRepository  = documentRepository;
        this.jobRepository       = jobRepository;
        this.flashcardRepository = flashcardRepository;
        this.pdfExtractor        = pdfExtractor;
        this.aiGenerator         = aiGenerator;
    }

    @Async("pdfProcessorExecutor")
    public void processPdf(Long documentId, byte[] pdfBytes, Long institutionId) {
        TenantContext.set(institutionId);
        try {
            var doc = documentRepository.findById(documentId).orElseThrow();
            documentRepository.save(doc.withStatus(DocumentStatus.PROCESSING));

            String text = pdfExtractor.extract(new ByteArrayInputStream(pdfBytes));

            doc = documentRepository.findById(documentId).orElseThrow();
            documentRepository.save(doc.withMarkdown(text));
        } catch (Exception e) {
            documentRepository.findById(documentId).ifPresent(d ->
                    documentRepository.save(d.withStatus(DocumentStatus.FAILED)));
        } finally {
            TenantContext.clear();
        }
    }

    @Async("pdfProcessorExecutor")
    public void generateFlashcards(Long jobId, String markdown, Long deckId, Long institutionId) {
        TenantContext.set(institutionId);
        try {
            var job = jobRepository.findById(jobId).orElseThrow();
            jobRepository.save(job.withRunning());

            List<String> chunks = DocumentService.chunk(markdown, DocumentService.CHUNK_SIZE);

            int totalInput = 0, totalOutput = 0;
            for (String chunk : chunks) {
                GenerationResult result = aiGenerator.generate(chunk);
                totalInput  += result.inputTokens();
                totalOutput += result.outputTokens();

                for (var s : result.flashcards()) {
                    flashcardRepository.save(
                            Flashcard.newInstance(institutionId, deckId, s.question(), s.answer()));
                }
            }

            job = jobRepository.findById(jobId).orElseThrow();
            jobRepository.save(job.withCompleted(totalInput, totalOutput));
        } catch (Exception e) {
            jobRepository.findById(jobId).ifPresent(j ->
                    jobRepository.save(j.withFailed(e.getMessage())));
        } finally {
            TenantContext.clear();
        }
    }
}
