package com.streakstudy.domain.exception;

public class RefreshTokenExpiredException extends DomainException {
    public RefreshTokenExpiredException() {
        super("El refresh token ha expirado.");
    }
}
