package com.streakstudy.infrastructure.event.listener;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.streakstudy.application.event.PasswordResetRequestedEvent;
import com.streakstudy.application.port.EmailSenderPort;

/**
 * Envia el correo de recuperacion de contrasena tras commit del request.
 *
 * <p>{@code AFTER_COMMIT}: si la persistencia del token falla, el correo
 * nunca se envia y el atacante no obtiene confirmacion lateral.</p>
 *
 * <p>{@code @Async("emailExecutor")}: el envio ocurre en el pool {@code email-*},
 * desacoplado del request original.</p>
 */
@Component
public class PasswordResetEmailListener {

    private final EmailSenderPort emailSender;
    private final String frontendUrl;

    public PasswordResetEmailListener(EmailSenderPort emailSender,
                                       @Value("${app.password-reset.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String link = frontendUrl + "/reset-password?token=" + event.plainToken();
        long minutes = Math.max(1, Duration.between(Instant.now(), event.expiresAt()).toMinutes());

        Map<String, Object> model = new HashMap<>();
        model.put("fullName", event.fullName());
        model.put("resetLink", link);
        model.put("expiresInMinutes", minutes);

        emailSender.sendHtml(
                event.email(),
                "Restablece tu contrasena en StreakStudy",
                "password-reset",
                model);
    }
}
