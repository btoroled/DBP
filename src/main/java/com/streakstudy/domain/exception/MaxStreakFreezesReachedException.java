package com.streakstudy.domain.exception;

public class MaxStreakFreezesReachedException extends DomainException {
    public MaxStreakFreezesReachedException() {
        super("El usuario ya tiene el límite máximo de 2 Streak Freezes.");
    }
}
