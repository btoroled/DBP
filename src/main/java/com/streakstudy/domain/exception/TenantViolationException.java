package com.streakstudy.domain.exception;

/**
 * Lanzada cuando una operacion intenta acceder o modificar datos de un tenant
 * que no es el tenant actual del contexto.
 *
 * <p>Es la senal de "fuga de datos" del sistema: si esta excepcion se
 * dispara, la regla de aislamiento esta funcionando correctamente.</p>
 */
public class TenantViolationException extends DomainException {
    public TenantViolationException(String message) {
        super(message);
    }

    public static TenantViolationException mismatch(Long expected, Long actual) {
        return new TenantViolationException(
            "Violacion de tenant: el contexto exige institutionId=%s pero el recurso es de institutionId=%s"
                .formatted(expected, actual));
    }

    public static TenantViolationException missingContext() {
        return new TenantViolationException(
            "No hay un institutionId en el TenantContext: operacion requiere autenticacion tenant-aware.");
    }
}
