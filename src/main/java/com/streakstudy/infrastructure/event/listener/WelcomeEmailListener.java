package com.streakstudy.infrastructure.event.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.streakstudy.application.event.UserRegisteredEvent;
import com.streakstudy.application.port.EmailSenderPort;

/**
 * Envia el correo de bienvenida tras el commit del registro.
 *
 * <p>{@code AFTER_COMMIT}: si el registro hace rollback (p.e. email duplicado
 * detectado por la unique constraint), el listener nunca se ejecuta — esto
 * cubre AC2.</p>
 *
 * <p>{@code @Async("emailExecutor")}: el envio ocurre en el thread pool
 * {@code email-*}, desacoplado del request original (cubre AC6).</p>
 */
@Component
public class WelcomeEmailListener {

    private final EmailSenderPort emailSender;

    public WelcomeEmailListener(EmailSenderPort emailSender) {
        this.emailSender = emailSender;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", event.fullName());
        model.put("email", event.email());
        emailSender.sendHtml(
                event.email(),
                "Bienvenido a StreakStudy",
                "welcome",
                model);
    }
}
