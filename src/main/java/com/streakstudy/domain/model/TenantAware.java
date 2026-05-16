package com.streakstudy.domain.model;

/**
 * Marca un objeto de dominio que pertenece a un tenant (institucion).
 *
 * <p>Toda entidad nueva del negocio (Course, Streak, Badge, Enrollment, etc.)
 * debe implementar esta interfaz. Su contraparte JPA debe extender
 * {@code TenantAwareJpaEntity} para heredar el listener de defensa que
 * valida que {@code institution_id} coincide con el {@code TenantContext}.</p>
 */
public interface TenantAware {
    Long institutionId();
}
