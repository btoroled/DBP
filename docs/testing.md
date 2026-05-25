> Parte de la documentación de [StreakStudy API](../README.md)

## Tests y Cobertura

Para ejecutar todas las suites de pruebas localmente:
`./mvnw test`

### Tests de Eventos y Email
| Test                                  | Tipo            | Verifica                                                    |
|---------------------------------------|-----------------|-------------------------------------------------------------|
| `EmailTemplateRendererTest`           | Unitario        | Renderizado de las 3 plantillas con datos de muestra        |
| `JavaMailEmailSenderAdapterTest`      | Unitario        | `MAIL_ENABLED=false` skip SMTP; errores no se propagan      |
| `WelcomeEmailListenerTest`            | Unitario        | El listener llama `sendHtml` con asunto y modelo correctos  |
| `AuthServiceEventTest`                | Event           | `register()` publica `UserRegisteredEvent`; no publica si rollback |
| `StoreServiceEventTest`               | Event           | `buyBadge()` publica `BadgeEarnedEvent`; no publica si excepción de dominio |
| `EmailIntegrationTest`                | Integración     | GreenMail SMTP real recibe el MIME message renderizado      |

---

### Cobertura General de la API

| Test                                    | Tipo           | Qué verifica                                        |
|-----------------------------------------|----------------|-----------------------------------------------------|
| `AuthServiceTest`                       | Unitario       | Registro, login, manejo de errores                  |
| `CourseServiceTest`                     | Unitario       | CRUD de cursos con tenant mock                      |
| `CourseServicePostgresContainerTest`    | Integración    | CRUD de cursos contra PostgreSQL real               |
| `InstitutionServiceTest`                | Unitario       | Creación de instituciones, código duplicado         |
| `LeaderboardServiceTest`                | Unitario       | Ranking filtrado por tenant                         |
| `RewardItemServiceTest`                 | Unitario       | Catálogo de items de la tienda                      |
| `StoreServiceTest`                      | Unitario       | Compra de streak freezes y badges, validaciones XP  |
| `StreakResetServiceTest`                | Unitario       | Reset de rachas de usuarios inactivos               |
| `UserProgressServiceTest`               | Unitario       | Registro de revisiones, acumulación de XP y racha   |
| `DocumentChunkingTest`                  | Unitario       | Chunking de texto: caso vacío, un chunk, múltiples, párrafos largos |
| `DocumentServiceTest`                   | Unitario       | Upload válido, PDF duplicado, archivo no-PDF, triggerGeneration, dedup de jobs |
| `DocumentControllerTest`                | Integración    | Endpoints de documentos (MockMvc): upload, status, job status |
| `CourseRepositoryAdapterTest`           | Integración    | Consultas JPA de cursos (H2)                        |
| `InstitutionRepositoryAdapterTest`      | Integración    | Consultas JPA de instituciones (H2)                 |
| `RewardItemRepositoryAdapterTest`       | Integración    | Consultas JPA de items de tienda (H2)               |
| `UserRepositoryAdapterTest`             | Integración    | Consultas JPA de usuarios (H2)                      |
| `UserRepositoryAdapterPostgresContainerTest` | Integración | Consultas JPA de usuarios contra PostgreSQL real   |
| `MultiTenancyIsolationTest`             | Integración    | Aislamiento real entre tenants (H2)                 |
| `JwtServiceTest`                        | Unitario       | Emisión y parsing de tokens JWT                     |
| `SecurityConfigTest`                    | Integración    | Endpoints públicos vs protegidos                    |
| `TenantContextTest`                     | Unitario       | ThreadLocal lifecycle, cross-tenant mode            |
| `AuthControllerTest`                    | Integración    | Endpoints de autenticación (MockMvc)                |
| `CourseControllerTest`                  | Integración    | Endpoints de cursos (MockMvc)                       |
| `InstitutionControllerTest`             | Integración    | Endpoints de instituciones (MockMvc)                |
| `LeaderboardControllerTest`             | Integración    | Endpoint de leaderboard (MockMvc)                   |
| `RewardItemControllerTest`              | Integración    | Endpoint de catálogo de recompensas (MockMvc)       |
| `UserProgressControllerTest`            | Integración    | Endpoints de progreso de usuario (MockMvc)          |
| `HealthControllerTest`                  | Integración    | Health endpoint                                     |
| `StreakStudyApplicationTests`           | Integración    | Carga del contexto Spring completo                  |

---
