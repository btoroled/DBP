package com.streakstudy.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.streakstudy.application.port.EmailSenderPort;

import jakarta.mail.internet.MimeMessage;

/**
 * Integracion del adapter SMTP contra GreenMail.
 *
 * <p>Usa el puerto SMTP de prueba de GreenMail con lifecycle por metodo
 * para evitar mensajes residuales entre tests.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.mail.enabled=true",
    "app.mail.from=test@streakstudy.test",
    "spring.mail.username=",
    "spring.mail.password=",
    "spring.mail.properties.mail.smtp.auth=false",
    "spring.mail.properties.mail.smtp.starttls.enable=false"
})
@ActiveProfiles("test")
class EmailIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication())
            .withPerMethodLifecycle(true);

    @DynamicPropertySource
    static void mailProps(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> ServerSetupTest.SMTP.getBindAddress());
        registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
    }

    @Autowired
    EmailSenderPort emailSender;

    @Test
    void shouldDeliverWelcomeMessageToSmtpWhenMailIsEnabled() throws Exception {
        emailSender.sendHtml(
                "alice@utec.edu",
                "Bienvenido a StreakStudy",
                "welcome",
                Map.of("fullName", "Alice Perez", "email", "alice@utec.edu"));

        greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] received = greenMail.getReceivedMessages();

        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Bienvenido a StreakStudy");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("alice@utec.edu");
        String body = GreenMailUtil.getBody(received[0]);
        assertThat(body).contains("Alice Perez");
    }
}
