package com.streakstudy.domain.exception;

public class RefreshTokenRevokedException extends DomainException {
    public RefreshTokenRevokedException() {
        super("El refresh token fue revocado.");
    }
}
