package com.streakstudy.infrastructure.event.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.streakstudy.application.event.FlashcardsGeneratedEvent;
import com.streakstudy.application.port.EmailSenderPort;

/**
 * Notifica al uploader cuando un job de generacion de flashcards completa.
 *
 * <p>Usa {@code @EventListener} (no {@code @TransactionalEventListener})
 * porque {@code DocumentProcessingService.generateFlashcards} se ejecuta
 * en su propio thread {@code @Async("pdfProcessorExecutor")} sin tx pegada
 * al request HTTP — el evento se publica una vez completado el procesamiento.</p>
 */
@Component
public class FlashcardsReadyEmailListener {

    private final EmailSenderPort emailSender;

    public FlashcardsReadyEmailListener(EmailSenderPort emailSender) {
        this.emailSender = emailSender;
    }

    @Async("emailExecutor")
    @EventListener
    public void onFlashcardsGenerated(FlashcardsGeneratedEvent event) {
        if (event.uploaderEmail() == null) {
            return;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", event.uploaderName());
        model.put("flashcardCount", event.flashcardCount());
        model.put("deckId", event.deckId());
        model.put("jobId", event.jobId());
        emailSender.sendHtml(
                event.uploaderEmail(),
                "Tus " + event.flashcardCount() + " flashcards estan listas",
                "flashcards-ready",
                model);
    }
}
