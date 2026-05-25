package com.streakstudy.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Badge {
    STREAK_STARTER("Estrella de Racha", "Por alcanzar una racha de 3 días"),
    XP_COLLECTOR("Coleccionista de XP", "Por acumular tus primeros 50 puntos de XP");

    private final String displayName;
    private final String description;

    Badge(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonCreator
    public static Badge fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El nombre de la insignia es obligatorio");
        }

        for (Badge badge : values()) {
            if (badge.name().equalsIgnoreCase(value) || badge.displayName.equalsIgnoreCase(value)) {
                return badge;
            }
        }

        throw new IllegalArgumentException("La insignia solicitada no existe en la tienda");
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}
