package com.streakstudy.domain.model;

/**
 * Roles del sistema.
 *
 * <p>{@code SUPER_ADMIN} es el unico rol cross-tenant (puede crear instituciones).
 * Los demas roles operan dentro de su propia institucion.</p>
 */
public enum UserRole {
    /** Usuario final (estudiante). */
    STUDENT,
    /** Profesor / facilitador, opera dentro de su institucion. */
    TEACHER,
    /** Administrador de la institucion. */
    INSTITUTION_ADMIN,
    /** Super administrador de la plataforma (cross-tenant). */
    SUPER_ADMIN
}
