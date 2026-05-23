# StreakStudy API

Plataforma de aprendizaje gamificada con IA, construida con Spring Boot 3 y arquitectura hexagonal. Soporta multitenancy a nivel de institución educativa.

---

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Dominio del Negocio](#dominio-del-negocio)
- [PDF → Flashcards con IA](#pdf--flashcards-con-ia)
- [API REST](#api-rest)
- [Multitenancy](#multitenancy)
- [Seguridad y Autenticación](#seguridad-y-autenticación)
- [Configuración](#configuración)
- [Ejecución con Docker](#ejecución-con-docker)
- [Ejecución Local](#ejecución-local)
- [Tests](#tests)
- [Equipo](#equipo)

---

## Descripción General

StreakStudy API es el backend de una plataforma educativa gamificada. Cada institución educativa opera como un tenant aislado: sus usuarios y cursos no son visibles desde otros tenants. La autenticación es por JWT y el contexto de tenant se propaga automáticamente en cada request.

Los estudiantes acumulan XP y rachas diarias completando revisiones. Pueden gastar su XP en la tienda para comprar streak freezes y badges. Un job diario reinicia las rachas de los estudiantes inactivos.

Los estudiantes también pueden subir documentos PDF y, una vez procesados, solicitar que la IA genere flashcards automáticamente a partir del contenido.

---

## Arquitectura

El proyecto sigue **Arquitectura Hexagonal (Ports & Adapters)**:

```
┌─────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE                    │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────┐ │
│  │   Web    │  │ Security │  │    Persistence      │ │
│  │(Controllers│ │(JWT,BCrypt│ │ (JPA, Adapters,    │ │
│  │  DTOs)   │  │ Config)  │  │  Mappers)          │ │
│  └────┬─────┘  └─────┬────┘  └─────────┬──────────┘ │
└───────┼──────────────┼────────────────┼─────────────┘
        │              │                │
┌───────▼──────────────▼────────────────▼─────────────┐
│                   APPLICATION                       │
│         Services  │  DTOs  │  Ports (Interfaces)   │
└───────────────────┼──────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────────────┐
│                    DOMAIN                           │
│    Entities  │  Repositories  │  Exceptions         │
│   (sin frameworks, Java puro)                       │
└─────────────────────────────────────────────────────┘
```

**Capas:**
- **Domain:** Entidades de negocio puras (sin anotaciones de frameworks).
- **Application:** Casos de uso (Services y UseCases) y puertos (interfaces).
- **Infrastructure:** Implementaciones técnicas: JPA, JWT, BCrypt, controladores REST, jobs.

---

## Stack Tecnológico

| Componente        | Tecnología                        |
|-------------------|-----------------------------------|
| Lenguaje          | Java 21                           |
| Framework         | Spring Boot 3.4.5                 |
| Persistencia      | Spring Data JPA + PostgreSQL 16   |
| Seguridad         | Spring Security + JWT (JJWT 0.12) |
| Build             | Maven 3.9                         |
| Contenedores      | Docker + Docker Compose           |
| Tests unitarios   | JUnit 5 + Mockito                 |
| Tests integración | @DataJpaTest + H2 + Testcontainers |
| Extracción PDF    | Apache PDFBox 3.0.3               |
| IA generativa     | Anthropic Claude Haiku (`claude-haiku-4-5-20251001`) |

---

## Estructura del Proyecto

```
src/main/java/com/streakstudy/
├── StreakStudyApplication.java
├── domain/
│   ├── model/
│   │   ├── User.java
│   │   ├── Course.java
│   │   ├── Institution.java
│   │   ├── Badge.java
│   │   ├── RewardItem.java
│   │   ├── Document.java          # Documento PDF subido
│   │   ├── DocumentStatus.java    # Enum: PENDING, PROCESSING, READY, FAILED
│   │   ├── AiGenerationJob.java   # Job de generación de flashcards
│   │   ├── AiGenerationJobStatus.java  # Enum: PENDING, RUNNING, COMPLETED, FAILED
│   │   ├── Deck.java              # Mazo de flashcards
│   │   ├── Flashcard.java         # Flashcard individual
│   │   ├── UserRole.java
│   │   └── TenantAware.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── CourseRepository.java
│   │   ├── InstitutionRepository.java
│   │   ├── RewardItemRepository.java
│   │   ├── DocumentRepository.java
│   │   ├── AiGenerationJobRepository.java
│   │   ├── DeckRepository.java
│   │   └── FlashcardRepository.java
│   └── exception/
│       ├── DomainException.java
│       ├── BadgeAlreadyOwnedException.java
│       ├── EmailAlreadyExistsException.java
│       ├── EntityNotFoundException.java
│       ├── InsufficientXpException.java
│       ├── InvalidCredentialsException.java
│       ├── MaxStreakFreezesReachedException.java
│       └── TenantViolationException.java
├── application/
│   ├── dto/                       # Request/Response records
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── CourseService.java
│   │   ├── DeckService.java             # CRUD de mazos de flashcards
│   │   ├── DocumentService.java         # Lógica de documentos y dedup
│   │   ├── DocumentProcessingService.java  # Procesamiento async (PDF + IA)
│   │   ├── FlashcardService.java        # CRUD de flashcards individuales
│   │   ├── InstitutionService.java
│   │   ├── LeaderboardService.java
│   │   ├── RewardItemService.java
│   │   ├── StoreService.java
│   │   ├── StreakResetService.java
│   │   └── UserProgressService.java
│   └── port/
│       ├── DocumentProcessingPort.java  # Interface para procesamiento async
│       ├── PdfTextExtractorPort.java    # Interface para extracción de texto
│       ├── AiFlashcardGeneratorPort.java  # Interface para generación IA
│       ├── FinishReviewUseCase.java
│       ├── GetUserProgressUseCase.java
│       ├── PasswordHasher.java
│       └── TokenIssuer.java
└── infrastructure/
    ├── ai/
    │   ├── PdfBoxTextExtractorAdapter.java      # PDFBox 3.x
    │   └── AnthropicFlashcardGeneratorAdapter.java  # Anthropic REST API
    ├── config/
    │   └── AsyncConfig.java          # ThreadPoolTaskExecutor para PDF/IA
    ├── job/                          # StreakResetJob (tarea diaria)
    ├── persistence/                  # JPA entities, repositories, adapters, mappers
    ├── security/                     # JwtService, JwtAuthenticationFilter, SecurityConfig
    ├── tenancy/                      # TenantContext, TenantAwareJpaEntity, EntityListener
    └── web/                          # Controllers, GlobalExceptionHandler
```

---

## Dominio del Negocio

### Entidades

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

---

## PDF → Flashcards con IA

### Flujo de Procesamiento

El pipeline está completamente asíncrono: el hilo HTTP siempre responde de inmediato (202 Accepted) y el trabajo pesado se ejecuta en un `ThreadPoolTaskExecutor` dedicado (`pdfProcessorExecutor`, 2–4 hilos).

```
Cliente                   API (hilo HTTP)              pdfProcessorExecutor
  │                            │                              │
  │── POST /upload ──────────► │                              │
  │                            │ valida tipo + tamaño         │
  │                            │ calcula SHA-256 (dedup)      │
  │                            │ guarda Document PENDING      │
  │                            │ ──── processPdf() ──────────►│
  │◄── 202 { documentId } ───  │                              │ extrae texto (PDFBox)
  │                            │                              │ → Document READY
  │── GET /status ───────────► │                              │
  │◄── { status: "READY" } ── │                              │
  │                            │                              │
  │── POST /generate ────────► │                              │
  │                            │ crea AiGenerationJob PENDING │
  │                            │ ──── generateFlashcards() ──►│
  │◄── 202 { jobId } ─────── │                              │ divide en chunks de 2000 chars
  │                            │                              │ llama a Claude Haiku por chunk
  │── GET /jobs/{jobId} ─────► │                              │ guarda Flashcards
  │◄── { status: "COMPLETED"} │                              │ → Job COMPLETED + tokens + costo
```

### Deduplicación de Documentos

Cada PDF subido se identifica por su hash SHA-256. Si un archivo idéntico ya existe, el endpoint devuelve el documento existente con `"duplicate": true` sin reprocesar.

### Tracking de Costos

Cada `AiGenerationJob` registra:
- `totalInputTokens` / `totalOutputTokens` acumulados sobre todos los chunks
- `estimatedCostUsd` calculado con los precios de Haiku: $0.80/M input · $4.00/M output

### Propagación del Tenant en Threads Async

Como los threads del pool no heredan el `ThreadLocal` del hilo HTTP, los métodos de `DocumentProcessingService` reciben `institutionId` como parámetro explícito y llaman a `TenantContext.set(institutionId)` al inicio, liberándolo en un bloque `finally`.

---

## API REST

### Autenticación (`/api/auth`) — Pública

| Método | Endpoint              | Descripción                     |
|--------|-----------------------|---------------------------------|
| POST   | `/api/auth/register`  | Registrar nuevo usuario         |
| POST   | `/api/auth/login`     | Iniciar sesión, obtener JWT     |

**Registro:**
```json
POST /api/auth/register
{
  "institutionId": 1,
  "email": "alumno@utec.edu.pe",
  "password": "12345678",
  "fullName": "Juan Pérez"
}
```

**Login:**
```json
POST /api/auth/login
{
  "email": "alumno@utec.edu.pe",
  "password": "12345678"
}
```

**Respuesta JWT:**
```json
{
  "token": "eyJ...",
  "expiresInSeconds": 3600,
  "userId": 1,
  "institutionId": 1,
  "email": "alumno@utec.edu.pe",
  "role": "STUDENT"
}
```

---

### Documentos PDF y Flashcards IA (`/api/documents`) — Requiere JWT

| Método | Endpoint                              | Descripción                                              |
|--------|---------------------------------------|----------------------------------------------------------|
| POST   | `/api/documents/upload`               | Subir PDF (multipart/form-data) → 202 Accepted          |
| GET    | `/api/documents/{id}/status`          | Consultar estado de procesamiento del documento          |
| GET    | `/api/documents/{id}/markdown`        | Obtener texto extraído del PDF (solo si READY)           |
| POST   | `/api/documents/{id}/generate-flashcards` | Disparar generación IA → 202 Accepted con jobId      |
| GET    | `/api/documents/jobs/{jobId}`         | Consultar estado del job IA (tokens, costo)              |
| GET    | `/api/documents/{id}/flashcards`      | Obtener las flashcards generadas para el documento       |

**Subir PDF:**
```
POST /api/documents/upload
Authorization: Bearer <jwt>
Content-Type: multipart/form-data

file=@apuntes.pdf
```

**Respuesta upload:**
```json
{
  "documentId": 42,
  "originalFilename": "apuntes.pdf",
  "status": "PENDING",
  "duplicate": false
}
```

**Estado del documento:**
```json
GET /api/documents/42/status

{
  "documentId": 42,
  "originalFilename": "apuntes.pdf",
  "status": "READY",
  "markdownAvailable": true
}
```

**Generar flashcards:**
```json
POST /api/documents/42/generate-flashcards
Authorization: Bearer <jwt>

{
  "deckId": 7
}
```

**Respuesta generación:**
```json
{
  "jobId": 77,
  "documentId": 42,
  "deckId": 7,
  "status": "PENDING",
  "totalInputTokens": 0,
  "totalOutputTokens": 0,
  "estimatedCostUsd": 0.0,
  "errorMessage": null
}
```

**Estado del job (finalizado):**
```json
GET /api/documents/jobs/77

{
  "jobId": 77,
  "documentId": 42,
  "deckId": 7,
  "status": "COMPLETED",
  "totalInputTokens": 1200,
  "totalOutputTokens": 480,
  "estimatedCostUsd": 0.00288,
  "errorMessage": null
}
```

---

### Mazos de Flashcards (`/api/decks`) — Requiere JWT

| Método | Endpoint           | Descripción                                  |
|--------|--------------------|----------------------------------------------|
| POST   | `/api/decks`       | Crear mazo (tenant actual)                   |
| GET    | `/api/decks`       | Listar mazos del tenant actual               |
| GET    | `/api/decks/{id}`  | Obtener mazo por ID (solo tenant actual)     |
| PUT    | `/api/decks/{id}`  | Actualizar nombre/descripción del mazo       |
| DELETE | `/api/decks/{id}`  | Eliminar mazo (solo tenant actual)           |

**Crear mazo:**
```json
POST /api/decks
Authorization: Bearer <jwt>

{
  "name": "Cálculo I - Derivadas",
  "description": "Mazo de práctica para el primer parcial"
}
```

**Respuesta:**
```json
{
  "id": 7,
  "institutionId": 1,
  "name": "Cálculo I - Derivadas",
  "description": "Mazo de práctica para el primer parcial",
  "createdAt": "2026-05-23T14:35:00Z"
}
```

**Actualizar mazo:**
```json
PUT /api/decks/7
Authorization: Bearer <jwt>

{
  "name": "Cálculo I - Derivadas e Integrales",
  "description": "Mazo de práctica para parciales 1 y 2"
}
```

---

### Flashcards (`/api/flashcards`) — Requiere JWT

| Método | Endpoint                          | Descripción                              |
|--------|-----------------------------------|------------------------------------------|
| POST   | `/api/flashcards`                 | Crear flashcard manualmente              |
| GET    | `/api/flashcards/deck/{deckId}`   | Listar flashcards de un mazo             |
| GET    | `/api/flashcards/{id}`            | Obtener flashcard por ID                 |
| PUT    | `/api/flashcards/{id}`            | Actualizar pregunta/respuesta            |
| DELETE | `/api/flashcards/{id}`            | Eliminar flashcard                       |

**Crear flashcard:**
```json
POST /api/flashcards
Authorization: Bearer <jwt>

{
  "deckId": 7,
  "question": "¿Cuál es la derivada de sin(x)?",
  "answer": "cos(x)"
}
```

**Respuesta:**
```json
{
  "id": 101,
  "deckId": 7,
  "question": "¿Cuál es la derivada de sin(x)?",
  "answer": "cos(x)",
  "createdAt": "2026-05-23T14:40:00Z"
}
```

**Actualizar flashcard:**
```json
PUT /api/flashcards/101
Authorization: Bearer <jwt>

{
  "question": "¿Cuál es la derivada de sen(x)?",
  "answer": "cos(x)"
}
```

---

### Cursos (`/api/courses`) — Requiere JWT

| Método | Endpoint            | Descripción                               |
|--------|---------------------|-------------------------------------------|
| POST   | `/api/courses`      | Crear curso (tenant actual)               |
| GET    | `/api/courses`      | Listar cursos del tenant actual           |
| GET    | `/api/courses/{id}` | Obtener curso por ID (solo tenant actual) |
| DELETE | `/api/courses/{id}` | Eliminar curso (solo tenant actual)       |

**Crear curso:**
```json
POST /api/courses
Authorization: Bearer <jwt>

{
  "name": "Cálculo I",
  "description": "Límites, derivadas e integrales"
}
```

---

### Instituciones (`/api/institutions`) — Pública

| Método | Endpoint                  | Descripción              |
|--------|---------------------------|--------------------------|
| POST   | `/api/institutions`       | Crear institución        |
| GET    | `/api/institutions/{id}`  | Obtener institución      |

**Crear institución:**
```json
POST /api/institutions
{
  "name": "Universidad de Ingeniería y Tecnología",
  "code": "utec"
}
```

---

### Progreso del Usuario (`/api/users/me/progress`) — Requiere JWT

| Método | Endpoint                           | Descripción                                      |
|--------|------------------------------------|--------------------------------------------------|
| GET    | `/api/users/me/progress`           | Obtener XP, racha y badges del usuario actual   |
| POST   | `/api/users/me/progress/review`    | Registrar finalización de una revisión (STUDENT) |

**Registrar revisión:**
```json
POST /api/users/me/progress/review
Authorization: Bearer <jwt>

{
  "courseId": 1,
  "xpEarned": 50
}
```

**Respuesta de progreso:**
```json
{
  "userId": 1,
  "xp": 350,
  "streak": 5,
  "streakFreezes": 1,
  "badges": ["FLAME", "SCHOLAR"]
}
```

---

### Tienda (`/api/store`) — Requiere JWT

| Método | Endpoint                  | Descripción                              |
|--------|---------------------------|------------------------------------------|
| POST   | `/api/store/streak-freeze` | Comprar un streak freeze con XP          |
| POST   | `/api/store/badges`        | Comprar un badge con XP (STUDENT)        |

**Comprar badge:**
```json
POST /api/store/badges
Authorization: Bearer <jwt>

{
  "badgeName": "FLAME"
}
```

---

### Catálogo de Recompensas (`/api/rewards`) — Requiere JWT

| Método | Endpoint       | Descripción                           |
|--------|----------------|---------------------------------------|
| GET    | `/api/rewards` | Listar todos los items de la tienda   |

---

### Leaderboard (`/api/leaderboard`) — Requiere JWT

| Método | Endpoint            | Descripción                                                      |
|--------|---------------------|------------------------------------------------------------------|
| GET    | `/api/leaderboard`  | Ranking de estudiantes del tenant actual, ordenado por XP desc  |

**Respuesta:**
```json
[
  { "id": 3, "fullName": "Ana García",  "streak": 12, "points": 850 },
  { "id": 1, "fullName": "Juan Pérez",  "streak": 5,  "points": 350 },
  { "id": 7, "fullName": "Luis Torres", "streak": 2,  "points": 200 }
]
```

---

### Salud (`/api/health`) — Pública

| Método | Endpoint       | Descripción      |
|--------|----------------|------------------|
| GET    | `/api/health`  | Estado de la API |

---

### Códigos de Error Estandarizados

Todos los errores siguen este formato:

```json
{
  "timestamp": "2026-05-17T12:00:00Z",
  "status": 404,
  "error": "not_found",
  "message": "Entity not found with id: 99"
}
```

| Código HTTP | `error`                            | Causa                             |
|-------------|------------------------------------|-----------------------------------|
| 400         | `validation_error`                 | Campos inválidos (con `errors[]`) |
| 400         | `bad_request`                      | Argumento inválido                |
| 401         | `invalid_credentials`              | Email o contraseña incorrectos    |
| 402         | `insufficient_xp`                  | XP insuficiente para comprar      |
| 403         | `tenant_violation`                 | Acceso a datos de otro tenant     |
| 404         | `not_found`                        | Recurso no encontrado             |
| 409         | `email_already_exists`             | Email ya registrado               |
| 409         | `institution_code_already_exists`  | Código de institución duplicado   |
| 409         | `badge_already_owned`              | El usuario ya posee ese badge     |
| 409         | `max_streak_freezes_reached`       | Límite de streak freezes alcanzado|

---

## Multitenancy

El aislamiento entre tenants se implementa en múltiples capas:

1. **JWT:** El token lleva el claim `institutionId` del usuario autenticado.
2. **TenantContext:** Al iniciar cada request, `JwtAuthenticationFilter` extrae el `institutionId` del JWT y lo almacena en un `ThreadLocal`. Se limpia al finalizar el request.
3. **Services:** Todos los métodos de servicios tenant-aware llaman a `TenantContext.requireInstitutionId()` antes de ejecutar.
4. **Queries JPA:** Todas las consultas de entidades tenant-aware incluyen `institutionId` como parámetro explícito.
5. **JPA Listener:** `TenantAwareEntityListener` valida en `@PrePersist` y `@PreUpdate` que el `institution_id` de la entidad coincide con el contexto actual. Si hay mismatch, lanza `TenantViolationException`.
6. **Threads async:** Los métodos de `DocumentProcessingService` reciben `institutionId` explícitamente y establecen el `TenantContext` en el thread del pool.

El resultado: si el tenant A intenta acceder a datos del tenant B, recibe `404` (para no filtrar la existencia del recurso) o `403`.

---

## Seguridad y Autenticación

- **Algoritmo JWT:** HS256 con secreto configurable (mínimo 32 caracteres).
- **Expiración del token:** 1 hora por defecto (configurable con `JWT_EXPIRATION_MS`).
- **Contraseñas:** Hasheadas con BCrypt.
- **API Stateless:** Sin sesiones, CSRF deshabilitado.
- **Contexto de seguridad:** `AuthenticatedUserPrincipal` almacenado en el `SecurityContext`; no se realizan consultas adicionales a la BD por request.

---

## Configuración

Copiar `.env.example` a `.env` y completar los valores:

| Variable             | Default                                           | Descripción                              |
|----------------------|---------------------------------------------------|------------------------------------------|
| `SERVER_PORT`        | `8080`                                            | Puerto del servidor                      |
| `DB_URL`             | `jdbc:postgresql://localhost:5432/streakstudy_db` | URL de base de datos                     |
| `DB_USER`            | `postgres`                                        | Usuario de BD                            |
| `DB_PASSWORD`        | `postgres`                                        | Contraseña de BD                         |
| `DB_POOL_MAX`        | `10`                                              | Máximo de conexiones (HikariCP)          |
| `DB_POOL_MIN`        | `2`                                               | Mínimo de conexiones idle                |
| `JPA_DDL`            | `update`                                          | DDL auto: `update`, `validate`, `none`   |
| `JPA_SHOW_SQL`       | `true`                                            | Loguear SQL en consola                   |
| `JWT_SECRET`         | *(inseguro por defecto)*                          | **Cambiar en producción** (min 32 chars) |
| `JWT_EXPIRATION_MS`  | `3600000`                                         | Expiración JWT en milisegundos (1h)      |
| `ANTHROPIC_API_KEY`  | *(requerido para IA)*                             | API key de Anthropic para Claude Haiku   |

---

## Ejecución con Docker

```bash
# Copiar variables de entorno
cp .env.example .env
# Editar .env y agregar ANTHROPIC_API_KEY

# Levantar PostgreSQL + API
docker compose up -d --build

# Ver logs
docker compose logs -f api

# Bajar el stack
docker compose down
```

La API queda disponible en `http://localhost:8080`.

**Smoke test básico:**
```bash
bash scripts/smoke-test.sh
```

**Smoke test multitenancy:**
```bash
bash scripts/multitenancy-smoke-test.sh
```

---

## Ejecución Local

Requiere Java 21 y PostgreSQL corriendo localmente.

```bash
# Compilar
./mvnw clean package -DskipTests

# Ejecutar
DB_URL=jdbc:postgresql://localhost:5432/streakstudy_db \
DB_USER=postgres \
DB_PASSWORD=postgres \
JWT_SECRET=mi-secreto-de-al-menos-32-caracteres-aqui \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run
```

---

## Tests

```bash
# Ejecutar todos los tests
./mvnw test
```

### Cobertura de Tests

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

## Equipo

Proyecto académico desarrollado para el curso **Desarrollo Basado en Plataformas (DBP)** — UTEC, 2026.
