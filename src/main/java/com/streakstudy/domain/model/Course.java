package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Curso de la plataforma. Tenant-aware: pertenece a una institucion.
 *
 * <p>Lo usamos como entidad de ejemplo para validar el aislamiento multi-tenant:
 * los usuarios de una institucion solo deben ver los cursos de su propia institucion.</p>
 */
public final class Course implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final String name;
    private final String description;
    private final Instant createdAt;

    public Course(Long id, Long institutionId, String name, String description, Instant createdAt) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.createdAt = createdAt;
    }

    public static Course newInstance(Long institutionId, String name, String description) {
        return new Course(null, institutionId, name, description, null);
    }

    public Long id() { return id; }
    @Override public Long institutionId() { return institutionId; }
    public String name() { return name; }
    public String description() { return description; }
    public Instant createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
