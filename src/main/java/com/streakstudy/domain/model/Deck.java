package com.streakstudy.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Deck implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final String name;
    private final String description;
    private final Instant createdAt;

    public Deck(
            Long id,
            Long institutionId,
            String name,
            String description,
            Instant createdAt
    ) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description == null ? "" : description;
        this.createdAt = createdAt;
    }

    public static Deck newInstance(
            Long institutionId,
            String name,
            String description
    ) {
        return new Deck(null, institutionId, name, description, null);
    }

    public Long id() {
        return id;
    }

    @Override
    public Long institutionId() {
        return institutionId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Deck other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}