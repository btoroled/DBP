# StreakStudy API

[![CI](https://github.com/btoroled/DBP/actions/workflows/ci.yml/badge.svg)](https://github.com/btoroled/DBP/actions/workflows/ci.yml)

Plataforma de aprendizaje gamificada con IA, construida con Spring Boot 3 y arquitectura hexagonal. Soporta multitenancy a nivel de institución educativa.

[![CI](https://github.com/btoroled/DBP/actions/workflows/ci.yml/badge.svg)](https://github.com/btoroled/DBP/actions/workflows/ci.yml)

**Curso:** CS 2031 Desarrollo Basado en Plataformas
**Integrantes:** 
* Benjamin Toro Leddihn
* Valentina Celeste Alvarez Beraun
* Daniel Sandoval Toro
* Alexander Leon Pantaleon
* Gloria Alfaro Quispe
---

## Índice

1. [Introducción](#1-introducción)
2. [Identificación del Problema o Necesidad](#2-identificación-del-problema-o-necesidad)
3. [Descripción de la Solución](#3-descripción-de-la-solución)
4. [Modelo de Entidades](#4-modelo-de-entidades)
5. [Testing y Manejo de Errores](#5-testing-y-manejo-de-errores)
6. [Medidas de Seguridad Implementadas](#6-medidas-de-seguridad-implementadas)
7. [Eventos y Asincronía](#7-eventos-y-asincronía)
8. [GitHub & Management](#8-github--management)
9. [Instalación y Despliegue](#9-instalación-y-despliegue)
10. [Conclusión](#10-conclusión)
11. [Apéndices](#11-apéndices)

---

## 1. Introducción

**Contexto:** En el entorno educativo actual, las instituciones enfrentan el desafío de mantener a los estudiantes comprometidos con sus rutinas de estudio autónomo. Además, la creación de material de repaso estructurado (como flashcards) consume mucho tiempo, lo que desmotiva su uso.

**Objetivos del Proyecto:**
* Desarrollar una API RESTful escalable utilizando Arquitectura Hexagonal.
* Implementar mecánicas de gamificación (rachas, puntos de experiencia y recompensas) para incentivar el estudio diario.
* Automatizar la generación de material de estudio procesando documentos PDF mediante Inteligencia Artificial.
* Garantizar la privacidad de los datos mediante una arquitectura Multi-Tenant estricta a nivel de base de datos.

## 2. Identificación del Problema o Necesidad

**Descripción del Problema:** Los estudiantes universitarios tienden a abandonar los hábitos de estudio progresivo, recurriendo a sesiones intensivas y poco efectivas justo antes de los exámenes. Además, no tienen muchas herramientas que automaticen la creación de recursos de aprendizaje a partir de sus apuntes o sílabos oficiales.

**Justificación:** Es relevante solucionar este problema porque la gamificación ha demostrado aumentar significativamente las tasas de retención de usuarios. Se utiliza la IA generativa para procesar PDFs automáticamente. Hacerlo bajo un modelo Multi-Tenant permite comercializar o distribuir el software a múltiples universidades asegurando que los datos de la "Universidad A" jamás se filtren a la "Universidad B".

## 3. Descripción de la Solución

El backend está diseñado bajo los principios de la **Arquitectura Hexagonal (Ports & Adapters)** para desacoplar la lógica de dominio de las implementaciones técnicas. > [Ver el detalle de la arquitectura y capas internas](docs/architecture.md).

```mermaid
flowchart TB

    subgraph Infrastructure
        WEB[Controllers / REST API]
        SEC[Security JWT]
        DB[(PostgreSQL)]
        EXT[Anthropic API]
    end

    subgraph Application
        SERVICES[Services / Use Cases]
        DTO[DTOs]
        PORTS[Ports]
    end

    subgraph Domain
        ENTITIES[Entities]
        REPOSITORIES[Repository Interfaces]
        EXCEPTIONS[Domain Exceptions]
    end

    WEB --> SERVICES
    SEC --> SERVICES
    SERVICES --> PORTS
    SERVICES --> REPOSITORIES
    PORTS --> EXT
    REPOSITORIES --> DB
```

**Funcionalidades Implementadas:**
* **Motor Multi-Tenant:** Aislamiento lógico de usuarios, cursos y documentos por institución educativa.
* **Gamificación:** Sistema de progresión donde los usuarios ganan XP, mantienen rachas diarias (streaks) y compran beneficios (Streak Freezes y Badges) en una tienda virtual.
* **Procesamiento de PDF e IA Generativa:** Endpoint asíncrono que extrae texto de archivos PDF y consulta un LLM para generar automáticamente mazos de flashcards interactivos.
* **API RESTful de Gestión:** CRUD completo para instituciones, cursos, mazos de flashcards y métricas de progreso de los estudiantes.> [Ver la referencia completa de endpoints y payloads JSON](docs/api-reference.md).

**Tecnologías Utilizadas:**
| Componente        | Tecnología                        |
|-------------------|-----------------------------------|
| Lenguaje          | Java 21                           |
| Framework         | Spring Boot 3.4.5                 |
| Persistencia      | Spring Data JPA + PostgreSQL 16   |
| Seguridad         | Spring Security + JWT (JJWT 0.12) |
| IA Generativa     | Anthropic Claude Haiku (REST)     |
| Build y Deploy    | Maven 3.9, Docker + Docker Compose|

## 4. Modelo de Entidades

El dominio se divide en Entidades Globales (como `Institution` y catálogo de `RewardItems`) y Entidades Tenant-Aware (como `User`, `Course`, `Deck` y `Document`). El aislamiento se logra inyectando automáticamente el `institutionId` a través de un `TenantAwareEntityListener` de JPA, impidiendo cruces de información.
```mermaid
erDiagram

    INSTITUTION ||--o{ USER : contains
    INSTITUTION ||--o{ COURSE : owns

    USER ||--o{ DOCUMENT : uploads
    USER ||--o{ DECK : creates
    USER }o--o{ BADGE : earns
    USER ||--o{ REFRESH_TOKEN : owns

    DOCUMENT ||--o{ AI_GENERATION_JOB : triggers

    DECK ||--o{ FLASHCARD : contains

    COURSE ||--o{ DECK : includes

    USER {
        long id
        string email
        string password
        string role
        int xp
        int streak
    }

    INSTITUTION {
        long id
        string name
        string code
    }

    DOCUMENT {
        long id
        string filename
        string status
        string sha256
    }

    FLASHCARD {
        long id
        string question
        string answer
    }
```

> [Ver el catálogo completo de entidades, roles y mecánicas](docs/entities.md).

## 5. Testing y Manejo de Errores

**Niveles de Testing Realizados:**
Se implementaron pruebas unitarias (JUnit 5 + Mockito) para aislar la lógica de negocio (como el cálculo de experiencia y rachas) y pruebas de integración (`@DataJpaTest` + H2/Testcontainers) para verificar las consultas a base de datos y el aislamiento estricto entre tenants.

**Resultados:** La suite de pruebas garantiza que las excepciones de dominio se lancen correctamente y que los flujos asíncronos respondan de manera adecuada.
> [Ver la matriz detallada de cobertura de pruebas](docs/testing.md).

**Manejo de Errores (Excepciones Globales):**
Para garantizar el funcionamiento correcto del sistema y ofrecer mensajes significativos en caso de un error, se centralizó el manejo de errores mediante un `GlobalExceptionHandler` utilizando el patrón `@ControllerAdvice`. Esto aísla la lógica de control de errores de los controladores REST y asegura que las trazas de los *stack traces* internos (que podrían exponer vulnerabilidades) nunca lleguen al cliente.

El sistema hereda de una clase base `DomainException` para estandarizar las validaciones de la capa de aplicación y dominio. Se manejan explícitamente estas excepciones críticas para proteger el modelo multi-tenant y mantener la integridad de los datos.

* **Excepciones de Seguridad y Aislamiento:**
    * `TenantViolationException`: Es la excepción más crítica del sistema. Se lanza inmediatamente si un usuario intenta acceder, modificar o eliminar un recurso (como un curso o documento) cuyo `institutionId` no coincide con el suyo. Manejar esta excepción es vital para prevenir fugas de datos entre diferentes universidades.
    * `InvalidCredentialsException`: Lanzada durante el flujo de autenticación para prevenir el acceso no autorizado sin revelar si el error provino del correo o de la contraseña, mitigando ataques de enumeración de usuarios.

* **Excepciones de Economía y Gamificación:**
  Manejar estas excepciones garantiza que los estudiantes no puedan vulnerar las reglas del sistema ni alterar su progreso de forma fraudulenta:
    * `InsufficientXpException`: Bloquea las transacciones en la tienda virtual si el estudiante intenta adquirir recompensas sin haber completado suficientes sesiones de estudio para acumular la experiencia requerida.
    * `BadgeAlreadyOwnedException`: Previene la compra duplicada de insignias.
    * `MaxStreakFreezesReachedException`: Limita la acumulación excesiva de escudos protectores de racha, obligando al usuario a mantener el hábito de estudio diario en lugar de depender únicamente de los escudos.

* **Excepciones de Integridad de Datos:**
    * `EmailAlreadyExistsException`: Evita colisiones de identidad en la base de datos durante el registro de nuevos usuarios, garantizando la restricción `UNIQUE` del esquema global.
    * `EntityNotFoundException`: Estandariza las respuestas HTTP 404 (Not Found) cuando un cliente solicita identificadores que no existen en su contexto, evitando que la aplicación rompa el flujo o lance excepciones genéricas de base de datos o punteros nulos (`NullPointerException`).

Todos los errores interceptados por el manejador global se transforman y devuelven bajo este formato estandarizado para facilitar el consumo uniforme por parte del frontend:

```json
{
  "timestamp": "2026-05-17T12:00:00Z",
  "status": 404,
  "error": "not_found",
  "message": "Entity not found with id: 99"
}
```
## 6. Medidas de Seguridad Implementadas

**Seguridad de Datos:**
* **Autenticación y Autorización:** Se utiliza Spring Security con tokens JWT (HMAC-SHA256). El `institutionId` viaja encriptado en los claims del token.
* **Cifrado:** Las contraseñas se almacenan mediante hashing con algoritmo BCrypt.
* **Gestión de Permisos:** Se aplican decoradores `@PreAuthorize` granulares en los controladores basados en una matriz estricta de roles (`STUDENT`, `TEACHER`, `INSTITUTION_ADMIN`).

**Prevención de Vulnerabilidades:**
* **Aislamiento de Sesiones:** La API es *stateless*, mitigando riesgos de secuestro de sesión.
* **Rate Limiting y Anti-enumeration:** Los endpoints de recuperación de contraseña responden siempre de manera neutra (202 Accepted) exista o no el correo, y cuentan con un límite de peticiones en memoria para prevenir ataques de fuerza bruta.

**Refresh tokens (nuevo flujo):**
```json
POST /api/v1/auth/login
{
  "accessToken": "eyJ...",
  "refreshToken": "0b9f...c7a",
  "expiresIn": 900,
  "userId": 1,
  "institutionId": 1,
  "email": "alumno@utec.edu.pe",
  "role": "STUDENT",
  "xp": 0
}
```

```json
POST /api/v1/auth/refresh
{
  "refreshToken": "0b9f...c7a"
}
```

```json
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
{
  "refreshToken": "0b9f...c7a"
}
```

**Recuperación de contraseña (Issue #10):**

| Método | Endpoint                          | Descripción                                 |
|--------|-----------------------------------|---------------------------------------------|
| POST   | `/api/v1/auth/password/forgot`    | Solicita email con link de reset -> 202     |
| POST   | `/api/v1/auth/password/reset`     | Confirma reset con token -> 204             |

```json
POST /api/v1/auth/password/forgot
{
  "email": "alumno@utec.edu.pe"
}
```
**Respuesta:** `202 Accepted` siempre - existe o no el email (anti-enumeration). Si el correo está habilitado (`MAIL_ENABLED=true`), se envía el link a `${FRONTEND_URL}/reset-password?token=...`.

```json
POST /api/v1/auth/password/reset
{
  "token": "<token-del-email>",
  "newPassword": "MiNuevaPassword123"
}
```
**Respuesta:** `204 No Content` cuando el token es válido.
**Errores:** `400 invalid_password_reset_token` si no existe o ya fue usado · `410 password_reset_token_expired` si pasó el TTL (30 min por defecto).

Características de seguridad:
- Token de **un solo uso**: tras un reset exitoso, el token se marca como usado.
- **Rotación**: solicitar un nuevo `/forgot` invalida todos los tokens previos activos del mismo email.
- Solo se almacena el **SHA-256** del token en `password_reset_tokens.token_hash`; el plaintext nunca toca la BD ni los logs.
- **Rate limit** in-memory: 5 requests/email/hora a `/forgot` (configurable). Excedido -> `429 Too Many Requests`.

---

### Documentos PDF y Flashcards IA (`/api/documents`) - Requiere JWT

| Método | Endpoint                              | Descripción                                              |
|--------|---------------------------------------|----------------------------------------------------------|
| POST   | `/api/documents/upload`               | Subir PDF (multipart/form-data) -> 202 Accepted          |
| GET    | `/api/documents/{id}/status`          | Consultar estado de procesamiento del documento          |
| GET    | `/api/documents/{id}/markdown`        | Obtener texto extraído del PDF (solo si READY)           |
| POST   | `/api/documents/{id}/generate-flashcards` | Disparar generación IA -> 202 Accepted con jobId      |
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

### Mazos de Flashcards (`/api/decks`) - Requiere JWT

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

### Flashcards (`/api/flashcards`) - Requiere JWT

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

## 7. Eventos y Asincronía

Para mantener tiempos de respuesta óptimos, la plataforma desacopla los *side-effects* utilizando `ApplicationEvent`.

**Implementación:**
* **Procesamiento de Archivos e IA:** La subida de un PDF responde inmediatamente con un estado `PENDING`. La extracción de texto y las llamadas de red al modelo de IA se ejecutan en un ThreadPool dedicado (`pdfProcessorExecutor`).
* **Notificaciones por Email:** Los correos transaccionales (bienvenida, confirmación de IA) se despachan utilizando listeners `@Async` y `@TransactionalEventListener(AFTER_COMMIT)` para asegurar que el sistema SMTP no ralentice el request original del usuario ni envíe correos si una transacción de base de datos falla.
* **Flujo Asíncrono de Procesamiento de PDF:**

```mermaid
sequenceDiagram

    participant Client
    participant API
    participant AsyncExecutor
    participant ClaudeAPI

    Client->>API: POST /documents/upload
    API->>API: Validar PDF y calcular SHA-256
    API->>API: Guardar Document en estado PENDING
    API->>AsyncExecutor: processPdfAsync(documentId, institutionId)
    API-->>Client: 202 Accepted

    AsyncExecutor->>AsyncExecutor: Extraer texto con PDFBox
    AsyncExecutor->>API: Actualizar Document a READY

    Client->>API: POST /documents/{id}/generate-flashcards
    API->>API: Crear AiGenerationJob en estado PENDING
    API->>AsyncExecutor: generateFlashcardsAsync(jobId, institutionId)
    API-->>Client: 202 Accepted

    AsyncExecutor->>ClaudeAPI: Generar flashcards por chunks
    ClaudeAPI-->>AsyncExecutor: Flashcards generadas
    AsyncExecutor->>API: Guardar Deck + Flashcards + costo
    AsyncExecutor->>API: Marcar Job como COMPLETED
```
> [Ver la matriz de eventos publicados y listeners](docs/events.md)


---

## 8. GitHub & Management

Para la gestión del proyecto, las tareas se dividieron en *issues* asignados individualmente a los miembros del equipo. Se establecieron *deadlines* semanales y cada ticket fue etiquetado por prioridad y capa técnica (infraestructura, seguridad, endpoints). Para asegurar la calidad del código, se implementó una política de *Pull Requests* (PRs) antes de integrarse a la rama principal.
Se implementó un flujo de Integración Continua (CI) con GitHub Actions. El *workflow* levanta un contenedor de PostgreSQL, compila el proyecto y ejecuta toda la suite de pruebas unitarias y de integración automáticamente en cada Push y Pull Request hacia las ramas principales. Además, se genera un reporte de cobertura automatizado utilizando el plugin JaCoCo.

> [Ver el flujo de trabajo detallado del workflow y variables de CI](docs/ci.md)

### Roles y Permisos (Issue #7)

Roles definidos en `UserRole`: `STUDENT`, `TEACHER`, `INSTITUTION_ADMIN`, `SUPER_ADMIN`.

Matriz de autorización aplicada con `@PreAuthorize` granular (controller + método):

| Endpoint                                       | Roles permitidos                                       |
|------------------------------------------------|--------------------------------------------------------|
| `POST /api/v1/auth/**`                         | público (register, login, refresh, forgot, reset)      |
| `POST /api/v1/auth/logout`                     | autenticado                                            |
| `POST /api/v1/institutions`                    | público (TODO: restringir a `SUPER_ADMIN` con seed)    |
| `GET  /api/v1/institutions/{id}`               | público                                                |
| `POST /api/v1/courses`                         | `TEACHER`, `INSTITUTION_ADMIN`, `SUPER_ADMIN`          |
| `GET  /api/v1/courses[/{id}]`                  | autenticado                                            |
| `DELETE /api/v1/courses/{id}`                  | `INSTITUTION_ADMIN`, `SUPER_ADMIN`                     |
| `POST /api/v1/decks`, `PUT/DELETE /decks/{id}` | `STUDENT`, `TEACHER`, `INSTITUTION_ADMIN`, `SUPER_ADMIN` |
| `GET  /api/v1/decks[/{id}]`                    | autenticado                                            |
| `POST/PUT/DELETE /api/v1/flashcards/**`        | `STUDENT`, `TEACHER`, `INSTITUTION_ADMIN`, `SUPER_ADMIN` |
| `GET  /api/v1/flashcards/**`                   | autenticado                                            |
| `POST /api/v1/store/streak-freeze`             | `STUDENT`                                              |
| `POST /api/v1/store/badges`                    | `STUDENT`                                              |
| `POST /api/v1/users/me/progress/review`        | `STUDENT`                                              |
| `GET  /api/v1/users/me/progress`               | autenticado                                            |
| `POST /api/v1/documents/**`, `GET /api/v1/documents/**` | autenticado                                   |
| `GET  /api/v1/rewards`                         | autenticado                                            |

Acceso sin la autoridad correcta -> `403 forbidden` (mapeado por `GlobalExceptionHandler` desde `AccessDeniedException`).

---

## 9. Instalación y Despliegue

La plataforma está dockerizada para facilitar el despliegue.

**Quick Start con Docker:**

```bash
cp .env.example .env
# Añadir credenciales (ej. ANTHROPIC_API_KEY) al archivo .env
docker compose up -d --build
```
La API quedará expuesta en `http://localhost:8080`.

**Links del Proyecto:**
* **URL de Producción:** [URL de Render/Railway]
* **Documentación Swagger:** http://localhost:8080/swagger-ui.html
* **Colección Postman:** [Link a JSON en el repo]

---

## 10. Conclusión

**Logros del Proyecto:** Se construyó un backend robusto capaz de gestionar múltiples instituciones educativas de forma hermética. Se logró integrar una mecánica funcional de gamificación y se automatizó con éxito la transformación de material bruto (PDFs) en recursos de estudio interactivos mediante Inteligencia Artificial.

**Aprendizajes Clave:** 
* La aplicación de Arquitectura Hexagonal facilita enormemente la sustitución de adaptadores tecnológicos (por ejemplo, cambiar la herramienta de IA sin alterar el *core*).
* El manejo de asincronía y el control del `ThreadLocal` para propagar el contexto de seguridad (y el Tenant) entre hilos concurrentes representa un desafío importante pero vital para el rendimiento.
* El diseño guiado por eventos (Event-Driven) resulta clave para construir flujos limpios y escalables (como el envío de emails).

**Trabajo Futuro:** 
* Expandir las mecánicas de recompensas con un sistema de ligas o tablas de clasificación (Leaderboard) globales (entre varias instituciones).
* Implementar analíticas descriptivas para que los profesores puedan medir el progreso de sus grupos.

---

## 11. Apéndices

### Referencias

- Documentación oficial de Spring Boot
- Documentación oficial de Spring Security
- Documentación oficial de PostgreSQL
- RFC 7519 JSON Web Token (JWT)
- OpenAPI Specification y Swagger UI
- Documentación de Testcontainers
- Documentación de Anthropic API

**Licencia:** Código liberado para uso académico bajo los términos del curso.
