package com.streakstudy.application.event;

/**
 * Evento publicado por {@code DocumentProcessingService.generateFlashcards}
 * cuando un job de IA completa exitosamente la generacion de flashcards.
 *
 * <p>Self-contained — incluye datos del uploader para que el listener no
 * tenga que consultar repositorios fuera del {@code TenantContext}.</p>
 */
public record FlashcardsGeneratedEvent(
        Long jobId,
        Long deckId,
        Long uploaderUserId,
        String uploaderEmail,
        String uploaderName,
        int flashcardCount,
        Long institutionId) {
}
