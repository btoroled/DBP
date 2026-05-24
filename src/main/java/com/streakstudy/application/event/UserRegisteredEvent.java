package com.streakstudy.application.event;

/**
 * Evento publicado por {@code AuthService.register} despues de persistir un nuevo usuario.
 *
 * <p>El evento es <b>self-contained</b>: carga todos los datos que el listener
 * necesita para componer el email de bienvenida. Esto es indispensable porque
 * el listener corre en un hilo distinto via {@code @Async("emailExecutor")}
 * y despues del commit, donde el {@code TenantContext} ya no esta disponible.</p>
 */
public record UserRegisteredEvent(
        Long userId,
        Long institutionId,
        String email,
        String fullName) {
}
