package com.streakstudy.application.event;

/**
 * Evento publicado por {@code StoreService.buyBadge} tras una compra exitosa.
 *
 * <p>Self-contained: incluye email y display name del badge para que el listener
 * pueda renderizar el correo sin consultar repos.</p>
 */
public record BadgeEarnedEvent(
        Long userId,
        Long institutionId,
        String email,
        String fullName,
        String badgeName,
        String badgeDisplayName,
        String badgeDescription) {
}
