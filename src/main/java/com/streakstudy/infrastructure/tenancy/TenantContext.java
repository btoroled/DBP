package com.streakstudy.infrastructure.tenancy;

import com.streakstudy.domain.exception.TenantViolationException;

/**
 * Contexto de tenant del request actual, almacenado en un {@link ThreadLocal}.
 *
 * <p>Es poblado por {@code JwtAuthenticationFilter} despues de validar el JWT,
 * leyendo el claim {@code institutionId}. Es limpiado al final del request
 * (via try/finally en el mismo filtro) para evitar fugas entre requests del
 * mismo thread pool.</p>
 *
 * <h2>Flujo de uso normal</h2>
 * <pre>
 * TenantContext.set(42L);
 * try {
 *     // ... logica de negocio que llama a CourseService.listForCurrentTenant() ...
 * } finally {
 *     TenantContext.clear();
 * }
 * </pre>
 *
 * <h2>Bypass para flujos cross-tenant</h2>
 * <p>Los endpoints de autenticacion y gestion de instituciones deben correr
 * con {@link #runCrossTenant} para no requerir el contexto.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CROSS_TENANT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() { }

    public static void set(Long institutionId) {
        if (institutionId == null) {
            throw new IllegalArgumentException("institutionId no puede ser null; usa clear() para limpiar");
        }
        CURRENT.set(institutionId);
    }

    public static void clear() {
        CURRENT.remove();
        CROSS_TENANT.remove();
    }

    /** Devuelve el tenant actual o {@code null} si no hay. */
    public static Long getInstitutionId() {
        return CURRENT.get();
    }

    /** Exige que haya un tenant en el contexto. Lanza si no, evitando fugas. */
    public static Long requireInstitutionId() {
        Long id = CURRENT.get();
        if (id == null && !CROSS_TENANT.get()) {
            throw TenantViolationException.missingContext();
        }
        if (id == null) {
            // En modo cross-tenant, pedir el tenant explicitamente es un bug del codigo
            throw new IllegalStateException(
                "requireInstitutionId() llamado en modo cross-tenant; revisa la logica del servicio.");
        }
        return id;
    }

    public static boolean hasTenant() {
        return CURRENT.get() != null;
    }

    public static boolean isCrossTenant() {
        return CROSS_TENANT.get();
    }

    /**
     * Ejecuta {@code action} marcando el thread como cross-tenant. Sirve para
     * flujos como registro, login y creacion de instituciones, donde el
     * concepto de "tenant del usuario" todavia no aplica.
     */
    public static <T> T runCrossTenant(java.util.function.Supplier<T> action) {
        Long previous = CURRENT.get();
        boolean previousFlag = CROSS_TENANT.get();
        CURRENT.remove();
        CROSS_TENANT.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            CROSS_TENANT.set(previousFlag);
            if (previous != null) {
                CURRENT.set(previous);
            }
        }
    }

    public static void runCrossTenant(Runnable action) {
        runCrossTenant(() -> { action.run(); return null; });
    }
}
