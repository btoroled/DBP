package com.streakstudy.domain.exception;

public class InsufficientXpException extends DomainException {
    public InsufficientXpException() {
        super("Puntos de experiencia (XP) insuficientes para realizar esta compra.");
    }
}
