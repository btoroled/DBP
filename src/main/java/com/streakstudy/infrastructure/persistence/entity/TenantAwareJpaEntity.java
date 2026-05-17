package com.streakstudy.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Clase base para toda entidad JPA tenant-aware.
 *
 * <p>Incluye:</p>
 * <ul>
 *   <li>Columna {@code institution_id NOT NULL} con indice.</li>
 *   <li>{@link TenantAwareEntityListener} que valida y/o asigna el tenant
 *       en {@code @PrePersist} y {@code @PreUpdate}.</li>
 * </ul>
 *
 * <p>Cualquier entidad nueva del proyecto (Streak, Badge, Enrollment, Lesson)
 * debe extender esta clase para heredar la regla de aislamiento.</p>
 */
@MappedSuperclass
@EntityListeners(TenantAwareEntityListener.class)
public abstract class TenantAwareJpaEntity {

    @Column(name = "institution_id", nullable = false, updatable = false)
    private Long institutionId;

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }
}
