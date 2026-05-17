package com.streakstudy.domain.exception;

public class EmailAlreadyExistsException extends DomainException {
    public EmailAlreadyExistsException(String email) {
        super("El email '%s' ya esta registrado.".formatted(email));
    }
}
