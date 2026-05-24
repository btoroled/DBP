package com.streakstudy.infrastructure.email;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.streakstudy.application.port.EmailSenderPort;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Adapter SMTP basado en JavaMail.
 *
 * <p>Si {@code app.mail.enabled=false}, no contacta al servidor SMTP y
 * simplemente loguea {@code [MAIL-DISABLED]}. Cuando esta habilitado, captura
 * cualquier {@link MailException} / {@link MessagingException} y la loguea como
 * ERROR sin propagarla — el flujo de negocio no debe romperse por un fallo
 * de correo (ver AC5 del Issue #1).</p>
 */
@Service
public class JavaMailEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailSenderAdapter.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer renderer;
    private final EmailProperties properties;

    public JavaMailEmailSenderAdapter(JavaMailSender mailSender,
                                       EmailTemplateRenderer renderer,
                                       EmailProperties properties) {
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.properties = properties;
    }

    @Override
    public void sendHtml(String to, String subject, String template, Map<String, Object> model) {
        String html = renderer.render(template, model);

        if (!properties.isEnabled()) {
            log.info("[MAIL-DISABLED] to={} subject='{}' template={}", to, subject, template);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to={} subject='{}'", to, subject);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send email to={} subject='{}': {}", to, subject, ex.getMessage());
        }
    }
}
