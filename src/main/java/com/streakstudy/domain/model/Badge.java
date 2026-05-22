package com.streakstudy.domain.model;

public enum Badge {
    STREAK_STARTER("Estrella de Racha", "Por alcanzar una racha de 3 días"),
    XP_COLLECTOR("Coleccionista de XP", "Por acumular tus primeros 50 puntos de XP");

    private final String displayName;
    private final String description;

    Badge(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}
