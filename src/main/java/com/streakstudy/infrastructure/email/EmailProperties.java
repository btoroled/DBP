package com.streakstudy.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de la app para envio de correo.
 *
 * <p>Lee {@code app.mail.from} y {@code app.mail.enabled} de
 * {@code application.properties} / env vars. Las propiedades SMTP nativas
 * ({@code spring.mail.*}) las maneja Spring Boot directamente.</p>
 */
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {

    private String from = "no-reply@streakstudy.com";

    /**
     * Si es {@code false}, el adapter loguea {@code [MAIL-DISABLED]} y no abre
     * conexion SMTP. Util para CI/tests/dev sin servidor SMTP real.
     */
    private boolean enabled = false;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
