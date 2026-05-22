package com.streakstudy.domain.exception;

public class BadgeAlreadyOwnedException extends DomainException {
    public BadgeAlreadyOwnedException(String badgeName) {
        super("El usuario ya posee la insignia: " + badgeName);
    }
}