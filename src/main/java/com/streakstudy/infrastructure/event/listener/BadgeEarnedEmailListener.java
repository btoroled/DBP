package com.streakstudy.infrastructure.event.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.streakstudy.application.event.BadgeEarnedEvent;
import com.streakstudy.application.port.EmailSenderPort;

/**
 * Notifica al estudiante cuando compra un badge.
 *
 * <p>{@code AFTER_COMMIT}: si la persistencia falla (p.e. validaciones de
 * dominio o concurrencia), no se envia correo.</p>
 */
@Component
public class BadgeEarnedEmailListener {

    private final EmailSenderPort emailSender;

    public BadgeEarnedEmailListener(EmailSenderPort emailSender) {
        this.emailSender = emailSender;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBadgeEarned(BadgeEarnedEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", event.fullName());
        model.put("badgeDisplayName", event.badgeDisplayName());
        model.put("badgeDescription", event.badgeDescription());
        emailSender.sendHtml(
                event.email(),
                "Ganaste el badge " + event.badgeDisplayName(),
                "badge-earned",
                model);
    }
}
