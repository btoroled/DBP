package com.streakstudy.domain.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Credenciales invalidas.");
    }
}
