package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Tenant raiz del sistema. Una Institucion agrupa usuarios, cursos y datos.
 *
 * <p>NO es tenant-aware: la propia Institution es la unidad de tenancy.</p>
 *
 * <p>Modelo de dominio en Java puro: sin anotaciones de JPA ni Spring.</p>
 */
public final class Institution {

    private final Long id;
    private final String name;
    private final String code;       // identificador corto unico (ej: "utec", "pucp")
    private final boolean active;
    private final Instant createdAt;

    public Institution(Long id, String name, String code, boolean active, Instant createdAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.code = Objects.requireNonNull(code, "code");
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Institution newInstance(String name, String code) {
        return new Institution(null, name, code, true, null);
    }

    public Long id() { return id; }
    public String name() { return name; }
    public String code() { return code; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Institution other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
