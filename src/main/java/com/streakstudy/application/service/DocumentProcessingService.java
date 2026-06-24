package com.streakstudy.application.service;

import com.streakstudy.application.event.FlashcardsGeneratedEvent;
import com.streakstudy.application.port.AiFlashcardGeneratorPort;
import com.streakstudy.application.port.AiFlashcardGeneratorPort.GenerationResult;
import com.streakstudy.application.port.PdfTextExtractorPort;
import com.streakstudy.domain.model.AiGenerationJob;
import com.streakstudy.domain.model.Document;
import com.streakstudy.domain.model.DocumentStatus;
import com.streakstudy.domain.model.Flashcard;
import com.streakstudy.domain.model.User;
import com.streakstudy.domain.repository.AiGenerationJobRepository;
import com.streakstudy.domain.repository.DocumentRepository;
import com.streakstudy.domain.repository.FlashcardRepository;
import com.streakstudy.domain.repository.UserRepository;
import com.streakstudy.application.port.DocumentProcessingPort;
import com.streakstudy.infrastructure.tenancy.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
public class DocumentProcessingService implements DocumentProcessingPort {

    private static final int MIN_CHUNK_CHARS = 300;

    private final DocumentRepository      documentRepository;
    private final AiGenerationJobRepository jobRepository;
    private final FlashcardRepository     flashcardRepository;
    private final UserRepository          userRepository;
    private final PdfTextExtractorPort    pdfExtractor;
    private final AiFlashcardGeneratorPort aiGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentProcessingService(DocumentRepository documentRepository,
                                     AiGenerationJobRepository jobRepository,
                                     FlashcardRepository flashcardRepository,
                                     UserRepository userRepository,
                                     PdfTextExtractorPort pdfExtractor,
                                     AiFlashcardGeneratorPort aiGenerator,
                                     ApplicationEventPublisher eventPublisher) {
        this.documentRepository  = documentRepository;
        this.jobRepository       = jobRepository;
        this.flashcardRepository = flashcardRepository;
        this.userRepository      = userRepository;
        this.pdfExtractor        = pdfExtractor;
        this.aiGenerator         = aiGenerator;
        this.eventPublisher      = eventPublisher;
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
            int totalFlashcards = 0;
            for (String chunk : chunks) {
                if (chunk.length() < MIN_CHUNK_CHARS) continue;
                GenerationResult result = aiGenerator.generate(chunk);
                totalInput  += result.inputTokens();
                totalOutput += result.outputTokens();

                for (var s : result.flashcards()) {
                    flashcardRepository.save(
                            Flashcard.newInstance(
                                    institutionId,
                                    deckId,
                                    s.question(),
                                    s.answer(),
                                    com.streakstudy.domain.model.Difficulty.MEDIUM
                            )
                    );
                    totalFlashcards++;
                }
            }

            job = jobRepository.findById(jobId).orElseThrow();
            AiGenerationJob completed = jobRepository.save(job.withCompleted(totalInput, totalOutput));

            publishCompletedEvent(completed, deckId, totalFlashcards, institutionId);
        } catch (Exception e) {
            jobRepository.findById(jobId).ifPresent(j ->
                    jobRepository.save(j.withFailed(e.getMessage())));
        } finally {
            TenantContext.clear();
        }
    }

    private void publishCompletedEvent(AiGenerationJob job, Long deckId, int flashcardCount, Long institutionId) {
        Document doc = documentRepository.findById(job.documentId()).orElse(null);
        if (doc == null) {
            return;
        }
        User uploader = userRepository.findById(doc.uploadedBy()).orElse(null);
        if (uploader == null) {
            return;
        }
        eventPublisher.publishEvent(new FlashcardsGeneratedEvent(
                job.id(),
                deckId,
                uploader.id(),
                uploader.email(),
                uploader.fullName(),
                flashcardCount,
                institutionId));
    }
}
