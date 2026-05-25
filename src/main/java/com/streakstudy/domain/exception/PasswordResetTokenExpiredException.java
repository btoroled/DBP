package com.streakstudy.domain.exception;

public class PasswordResetTokenExpiredException extends DomainException {
    public PasswordResetTokenExpiredException() {
        super("Token de recuperacion expirado.");
    }
}
