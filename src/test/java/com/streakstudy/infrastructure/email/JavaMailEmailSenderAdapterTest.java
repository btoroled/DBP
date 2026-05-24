package com.streakstudy.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class JavaMailEmailSenderAdapterTest {

    @Mock JavaMailSender mailSender;
    @Mock EmailTemplateRenderer renderer;

    private EmailProperties props;
    private JavaMailEmailSenderAdapter adapter;

    @BeforeEach
    void setup() {
        props = new EmailProperties();
        props.setFrom("no-reply@streakstudy.test");
        adapter = new JavaMailEmailSenderAdapter(mailSender, renderer, props);
    }

    @Test
    void shouldNotContactSmtpWhenMailIsDisabled() {
        props.setEnabled(false);
        when(renderer.render("welcome", Map.of("fullName", "Alice")))
                .thenReturn("<p>html</p>");

        adapter.sendHtml("alice@utec.edu", "Bienvenida", "welcome", Map.of("fullName", "Alice"));

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendMimeMessageWhenMailIsEnabled() {
        props.setEnabled(true);
        when(renderer.render("welcome", Map.of("fullName", "Alice")))
                .thenReturn("<p>html</p>");
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        adapter.sendHtml("alice@utec.edu", "Bienvenida", "welcome", Map.of("fullName", "Alice"));

        verify(mailSender).send(message);
    }

    @Test
    void shouldNotPropagateExceptionWhenSmtpFails() {
        props.setEnabled(true);
        when(renderer.render("welcome", Map.of("fullName", "Alice")))
                .thenReturn("<p>html</p>");
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
                adapter.sendHtml("alice@utec.edu", "Bienvenida", "welcome", Map.of("fullName", "Alice")));
    }
}
