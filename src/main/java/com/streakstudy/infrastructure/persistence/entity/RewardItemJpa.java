package com.streakstudy.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla de recompensas. Tenant-aware.
 */
@Entity
@Table(
        name = "reward_items",
        indexes = {
                @Index(name = "ix_reward_items_institution", columnList = "institution_id")
        }
)
public class RewardItemJpa extends TenantAwareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "cost_in_points", nullable = false)
    private Integer costInPoints;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private InstitutionJpa institution;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCostInPoints() { return costInPoints; }
    public void setCostInPoints(Integer costInPoints) { this.costInPoints = costInPoints; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public InstitutionJpa getInstitution() { return institution; }
}