> Parte de la documentación de [StreakStudy API](../README.md).
> 

# Modelo de Entidades

En este documento se detalla la estructura del modelo de datos de **StreakStudy**, las reglas de negocio en las entidades del dominio, el acceso basado en roles y el entorno Multi-Tenant implementado.

---

## 1. Catálogo de Entidades del Sistema

El modelo se divide en dos categorías según su comportamiento dentro de la arquitectura multi-tenant:
1. **Global Entities (Cross-Tenant):** Componentes compartidos de la plataforma que no pertenecen a ninguna institución en particular.
2. **Tenant-Aware Entities:** Componentes cuyos datos pertenecen exclusivamente a una institución y están completamente aislados mediante un identificador único (`institutionId`).

| Entidad            | Tenant-Aware | Descripción                                              |
|--------------------|:------------:|----------------------------------------------------------|
| `Institution`      | No (raíz)    | Institución educativa (`code`: "utec", "pucp")           |
| `User`             | Sí           | Usuario con XP, racha diaria y badges                    |
| `Course`           | Sí           | Curso perteneciente a una institución                    |
| `Badge`            | No           | Insignia comprable con XP                                |
| `RewardItem`       | No           | Item del catálogo de la tienda                           |
| `Document`         | Sí           | PDF subido; pasa por estados PENDING → PROCESSING → READY |
| `AiGenerationJob`  | No           | Registro de un trabajo de generación IA con tracking de tokens y costo |
| `Deck`             | Sí           | Mazo de flashcards                                       |
| `Flashcard`        | Sí           | Pregunta + respuesta generada por IA                     |

### Roles de Usuario

| Rol                  | Descripción                            |
|----------------------|----------------------------------------|
| `STUDENT`            | Alumno dentro de una institución       |
| `TEACHER`            | Facilitador dentro de una institución  |
| `INSTITUTION_ADMIN`  | Administrador de la institución        |
| `SUPER_ADMIN`        | Administrador cross-tenant             |

### Mecánicas de Gamificación

- **XP:** Los estudiantes ganan XP al completar revisiones (`POST /api/users/me/progress/review`).
- **Racha:** Días consecutivos con al menos una revisión completada. Un job diario reinicia la racha de estudiantes inactivos.
- **Streak Freeze:** Item comprable que protege la racha ante un día sin actividad.
- **Badges:** Insignias comprables con XP en la tienda.


### Reglas de Negocio Incorporadas:

* **Mecánica de XP:** El estudiante acumula puntos de experiencia al revisar flashcards con éxito. El progreso es estrictamente acumulativo.
* **Control de Rachas (*Streaks*):** Si `lastActiveDate` es igual a ayer, la racha se incrementa en $1$ al realizar actividades. Si pasa más de un día sin actividad, se evalúa el consumo de `streakFreezes` (escudos). Si el contador de escudos es $0$, la racha se reinicia automáticamente a $0$.

## Mecanismo Técnico de Aislamiento Multi-Tenant

Para mitigar riesgos de filtración o corrupción de datos entre diferentes universidades (*Data Leaks*), se implementó una estrategia de **Aislamiento a Nivel de Aplicación con Base de Datos Compartida** (*Shared Database, Discriminator Column*).

### El Flujo de Protección en Persistencia (JPA):

Cada vez que una entidad marcada como `Tenant-Aware` interactúa con la base de datos a través de Hibernate, se activa el ciclo de vida de JPA manejado por un Listener centralizado (`TenantAwareEntityListener`).

* **Inyección Automática (`@PrePersist`):** Al guardar un nuevo registro, el listener intercepta el objeto, extrae el `institutionId` validado en el contexto del JWT del hilo actual (`TenantContext.getCurrentTenant()`) y lo asigna automáticamente al campo de la entidad antes de ejecutar el `INSERT`. El cliente no puede forzar un ID de institución falso.
* **Validación Antifraude (`@PreUpdate` / `@PreRemove`):** Antes de ejecutar un `UPDATE` o `DELETE`, el listener compara el `institutionId` del registro existente en la base de datos con el del usuario autenticado en la sesión actual. Si los identificadores no coinciden, la operación se aborta de inmediato lanzando una excepción de seguridad de datos:

```java
public class TenantAwareEntityListener {
    @PreUpdate
    @PreRemove
    public void validateTenantIsolation(Object entity) {
        if (entity instanceof TenantAware tenantAwareEntity) {
            Long currentTenant = TenantContext.getCurrentTenant();
            if (!tenantAwareEntity.getInstitutionId().equals(currentTenant)) {
                throw new TenantViolationException("Acceso no autorizado: Intento de cruce de datos Multi-Tenant.");
            }
        }
    }
}
```
