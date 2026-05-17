package com.streakstudy.application.port;

import com.streakstudy.domain.model.User;

/**
 * Puerto para emision de tokens. La implementacion vive en
 * {@code infrastructure.security} (JWT).
 *
 * <p>Todo token emitido debe incluir como claim el {@code institutionId} del
 * usuario; ese claim es el que reconstituye el {@code TenantContext} en cada
 * request autenticado.</p>
 */
public interface TokenIssuer {
    String issue(User user);
    long expirationSeconds();
}
