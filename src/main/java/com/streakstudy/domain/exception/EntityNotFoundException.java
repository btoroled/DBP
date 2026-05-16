package com.streakstudy.domain.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entity, Object id) {
        super("%s con id=%s no encontrado".formatted(entity, id));
    }
    public EntityNotFoundException(String message) {
        super(message);
    }
}
