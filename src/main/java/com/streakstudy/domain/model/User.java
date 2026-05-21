package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Usuario de la plataforma. Tenant-aware: cada usuario pertenece a una institucion.
 *
 * <p>El campo {@code passwordHash} es el hash BCrypt (no la contrasena plana).</p>
 */
public final class User implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UserRole role;
    private final Instant createdAt;

    // ========================================================
    // NUEVOS CAMPOS AGREGADOS PARA TU PARTE (UI #11, #12, #15)
    // ========================================================
    private final Integer streak;
    private final Integer points;

    public User(Long id,
                Long institutionId,
                String email,
                String passwordHash,
                String fullName,
                UserRole role,
                Instant createdAt,
                Integer streak,
                Integer points) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.email = Objects.requireNonNull(email, "email").toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = createdAt;
        this.streak = streak != null ? streak : 0;
        this.points = points != null ? points : 0;
    }

    public static User newStudent(Long institutionId, String email, String passwordHash, String fullName) {
        return new User(null, institutionId, email, passwordHash, fullName, UserRole.STUDENT, null, 0, 0);
    }

    // Métodos para actualizar los puntos creando una nueva instancia (Inmutabilidad)
    public User withStreakAndPoints(Integer newStreak, Integer newPoints) {
        return new User(this.id, this.institutionId, this.email, this.passwordHash, this.fullName, this.role, this.createdAt, newStreak, newPoints);
    }

    public Long id() { return id; }
    @Override public Long institutionId() { return institutionId; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String fullName() { return fullName; }
    public UserRole role() { return role; }
    public Instant createdAt() { return createdAt; }

    // NUEVOS MÉTODOS DE ACCESO ESTILO TU GRUPO (SIn "GET")
    public Integer streak() { return streak; }
    public Integer points() { return points; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}