package com.streakstudy.domain.model;

import java.util.Objects;

/**
 * Representa un articulo canjeable en la tienda de recompensas.
 * Cada institucion (tenant) gestiona sus propios premios de manera aislada.
 */
public final class RewardItem implements TenantAware {

    private final Long id;
    private final Long institutionId;
    private final String title;
    private final String description;
    private final Integer costInPoints;
    private final Integer stock;

    public RewardItem(Long id, Long institutionId, String title, String description, Integer costInPoints, Integer stock) {
        this.id = id;
        this.institutionId = Objects.requireNonNull(institutionId, "institutionId");
        this.title = Objects.requireNonNull(title, "title");
        this.description = description;
        this.costInPoints = Objects.requireNonNull(costInPoints, "costInPoints");
        this.stock = Objects.requireNonNull(stock, "stock");
    }

    public Long id() { return id; }
    @Override public Long institutionId() { return institutionId; }
    public String title() { return title; }
    public String description() { return description; }
    public Integer costInPoints() { return costInPoints; }
    public Integer stock() { return stock; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RewardItem other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}