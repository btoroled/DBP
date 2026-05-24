package com.streakstudy.domain.exception;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("Refresh token invalido.");
    }
}
