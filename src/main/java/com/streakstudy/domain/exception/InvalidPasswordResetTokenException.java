package com.streakstudy.domain.exception;

public class InvalidPasswordResetTokenException extends DomainException {
    public InvalidPasswordResetTokenException() {
        super("Token de recuperacion invalido.");
    }
}
