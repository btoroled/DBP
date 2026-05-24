package com.streakstudy.application.port;

import java.util.Map;

/**
 * Puerto de salida para envio de correos electronicos.
 *
 * <p>El adapter por defecto ({@code JavaMailEmailSenderAdapter}) usa JavaMail +
 * Thymeleaf. Implementaciones alternas (test, log-only, SES, etc.) pueden
 * sustituirlo sin tocar la capa de aplicacion.</p>
 */
public interface EmailSenderPort {

    /**
     * Envia un correo HTML renderizando la plantilla Thymeleaf indicada.
     *
     * @param to       direccion destino
     * @param subject  asunto del correo
     * @param template nombre de la plantilla (sin extension) bajo {@code templates/email/}
     * @param model    variables disponibles en la plantilla
     */
    void sendHtml(String to, String subject, String template, Map<String, Object> model);
}
