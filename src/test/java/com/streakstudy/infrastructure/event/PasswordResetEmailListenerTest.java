package com.streakstudy.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.event.PasswordResetRequestedEvent;
import com.streakstudy.application.port.EmailSenderPort;
import com.streakstudy.infrastructure.event.listener.PasswordResetEmailListener;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailListenerTest {

    @Mock EmailSenderPort emailSender;

    @Test
    void shouldSendPasswordResetEmailWithLinkPointingToFrontend() {
        PasswordResetEmailListener listener = new PasswordResetEmailListener(emailSender, "http://localhost:5173");
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                7L, "alice@utec.edu", "Alice Perez", "plain-token-xyz",
                Instant.now().plus(30, ChronoUnit.MINUTES));

        listener.onPasswordResetRequested(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailSender).sendHtml(
                org.mockito.ArgumentMatchers.eq("alice@utec.edu"),
                org.mockito.ArgumentMatchers.eq("Restablece tu contrasena en StreakStudy"),
                org.mockito.ArgumentMatchers.eq("password-reset"),
                modelCaptor.capture());

        Map<String, Object> model = modelCaptor.getValue();
        assertThat(model).containsEntry("fullName", "Alice Perez");
        assertThat((String) model.get("resetLink"))
                .startsWith("http://localhost:5173/reset-password?token=")
                .contains("plain-token-xyz");
        assertThat((Long) model.get("expiresInMinutes")).isBetween(28L, 31L);
    }
}
