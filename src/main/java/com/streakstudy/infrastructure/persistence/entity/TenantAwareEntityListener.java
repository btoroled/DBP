package com.streakstudy.infrastructure.persistence.entity;

import com.streakstudy.domain.exception.TenantViolationException;
import com.streakstudy.infrastructure.tenancy.TenantContext;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Listener JPA de defensa en escritura.
 *
 * <h2>Reglas</h2>
 * <ol>
 *   <li>Si la entidad no tiene {@code institution_id} y hay {@code TenantContext},
 *       lo asigna automaticamente (red de seguridad ante olvidos del servicio).</li>
 *   <li>Si la entidad tiene {@code institution_id} y el {@code TenantContext}
 *       difiere, aborta con {@link TenantViolationException}. Esta es la
 *       senal clara de un intento de fuga de datos en el codigo.</li>
 *   <li>Si la operacion esta dentro de {@code TenantContext.runCrossTenant(...)},
 *       no valida nada: el llamador asume la responsabilidad.</li>
 * </ol>
 */
public class TenantAwareEntityListener {

    @PrePersist
    public void prePersist(TenantAwareJpaEntity entity) {
        validateAndAssign(entity, "persistir");
    }

    @PreUpdate
    public void preUpdate(TenantAwareJpaEntity entity) {
        validateAndAssign(entity, "actualizar");
    }

    private void validateAndAssign(TenantAwareJpaEntity entity, String operacion) {
        if (TenantContext.isCrossTenant()) {
            // El llamador (ej. registro de usuario) sabe lo que hace.
            return;
        }

        Long contextTenant = TenantContext.getInstitutionId();
        Long entityTenant = entity.getInstitutionId();

        if (contextTenant == null && entityTenant == null) {
            throw TenantViolationException.missingContext();
        }

        if (entityTenant == null) {
            entity.setInstitutionId(contextTenant);
            return;
        }

        if (contextTenant != null && !contextTenant.equals(entityTenant)) {
            throw new TenantViolationException(
                "Intento de %s entidad de institutionId=%s desde contexto institutionId=%s"
                    .formatted(operacion, entityTenant, contextTenant));
        }
    }
}
