package com.streakstudy.infrastructure.security;

import com.streakstudy.domain.model.UserRole;

/**
 * Principal almacenado en {@code Authentication.getPrincipal()} para requests
 * autenticados. Lleva todo lo necesario para los controllers sin necesidad de
 * volver a leer la base de datos.
 */
public record AuthenticatedUserPrincipal(
    Long userId,
    Long institutionId,
    String email,
    UserRole role
) { }
