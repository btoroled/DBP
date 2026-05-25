package com.streakstudy.application.event;

import java.time.Instant;

/**
 * Evento publicado por {@code PasswordResetService.requestReset} tras
 * persistir el hash del token.
 *
 * <p>Carga el {@code plainToken} en memoria solo el tiempo suficiente para
 * que el listener arme el link de reset. No se persiste en claro ni se
 * loguea.</p>
 */
public record PasswordResetRequestedEvent(
        Long userId,
        String email,
        String fullName,
        String plainToken,
        Instant expiresAt) {
}
