package com.streakstudy.domain.exception;

/**
 * Excepcion base del dominio. Las subclases representan errores de reglas de
 * negocio (no errores tecnicos).
 */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
}
