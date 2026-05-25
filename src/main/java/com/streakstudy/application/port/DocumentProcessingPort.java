package com.streakstudy.application.port;

public interface DocumentProcessingPort {
    void processPdf(Long documentId, byte[] pdfBytes, Long institutionId);
    void generateFlashcards(Long jobId, String markdown, Long deckId, Long institutionId);
}
