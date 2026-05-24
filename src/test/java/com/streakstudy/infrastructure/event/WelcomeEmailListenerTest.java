package com.streakstudy.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streakstudy.application.event.UserRegisteredEvent;
import com.streakstudy.application.port.EmailSenderPort;
import com.streakstudy.infrastructure.event.listener.WelcomeEmailListener;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailListenerTest {

    @Mock EmailSenderPort emailSender;
    @InjectMocks WelcomeEmailListener listener;

    @Test
    void shouldSendWelcomeEmailWhenUserRegisteredEventReceived() {
        UserRegisteredEvent event = new UserRegisteredEvent(7L, 1L, "alice@utec.edu", "Alice Perez");

        listener.onUserRegistered(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailSender).sendHtml(
                org.mockito.ArgumentMatchers.eq("alice@utec.edu"),
                org.mockito.ArgumentMatchers.eq("Bienvenido a StreakStudy"),
                org.mockito.ArgumentMatchers.eq("welcome"),
                modelCaptor.capture());

        Map<String, Object> model = modelCaptor.getValue();
        assertThat(model).containsEntry("fullName", "Alice Perez");
        assertThat(model).containsEntry("email", "alice@utec.edu");
    }
}
