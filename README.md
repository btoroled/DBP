# StreakStudy API

Plataforma de aprendizaje gamificada con IA, construida con Spring Boot 3 y arquitectura hexagonal. Soporta multitenancy a nivel de institución educativa.

---

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Dominio del Negocio](#dominio-del-negocio)
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
- **Application:** Casos de uso (Services) y puertos (interfaces).
- **Infrastructure:** Implementaciones técnicas: JPA, JWT, BCrypt, controladores REST.

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
| Tests integración | @DataJpaTest + H2                 |

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
│   │   ├── UserRole.java          # Enum: STUDENT, TEACHER, INSTITUTION_ADMIN, SUPER_ADMIN
│   │   └── TenantAware.java       # Interface marker para entidades con tenant
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── CourseRepository.java
│   │   └── InstitutionRepository.java
│   └── exception/
│       ├── DomainException.java
│       ├── EmailAlreadyExistsException.java
│       ├── EntityNotFoundException.java
│       ├── InvalidCredentialsException.java
│       └── TenantViolationException.java
├── application/
│   ├── dto/                       # Request/Response records
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── CourseService.java
│   │   └── InstitutionService.java
│   └── port/
│       ├── PasswordHasher.java
│       └── TokenIssuer.java
└── infrastructure/
    ├── persistence/               # JPA entities, repositories, adapters, mappers
    ├── security/                  # JwtService, JwtAuthenticationFilter, SecurityConfig
    ├── tenancy/                   # TenantContext, TenantAwareJpaEntity, EntityListener
    └── web/                       # Controllers, GlobalExceptionHandler
```

---

## Dominio del Negocio

### Entidades

| Entidad       | Tenant-Aware | Descripción                                        |
|---------------|:------------:|----------------------------------------------------|
| `Institution` | No (raíz)    | Institución educativa (`code`: "utec", "pucp")     |
| `User`        | Sí           | Usuario con email globalmente único                |
| `Course`      | Sí           | Curso perteneciente a una institución              |

### Roles de Usuario

| Rol                  | Descripción                            |
|----------------------|----------------------------------------|
| `STUDENT`            | Alumno dentro de una institución       |
| `TEACHER`            | Facilitador dentro de una institución  |
| `INSTITUTION_ADMIN`  | Administrador de la institución        |
| `SUPER_ADMIN`        | Administrador cross-tenant             |

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
| 403         | `tenant_violation`                 | Acceso a datos de otro tenant     |
| 404         | `not_found`                        | Recurso no encontrado             |
| 409         | `email_already_exists`             | Email ya registrado               |
| 409         | `institution_code_already_exists`  | Código de institución duplicado   |

---

## Multitenancy

El aislamiento entre tenants se implementa en múltiples capas:

1. **JWT:** El token lleva el claim `institutionId` del usuario autenticado.
2. **TenantContext:** Al iniciar cada request, `JwtAuthenticationFilter` extrae el `institutionId` del JWT y lo almacena en un `ThreadLocal`. Se limpia al finalizar el request.
3. **Services:** Todos los métodos de `CourseService` llaman a `TenantContext.requireInstitutionId()` antes de ejecutar.
4. **Queries JPA:** Todas las consultas de entidades tenant-aware incluyen `institutionId` como parámetro explícito.
5. **JPA Listener:** `TenantAwareEntityListener` valida en `@PrePersist` y `@PreUpdate` que el `institution_id` de la entidad coincide con el contexto actual. Si hay mismatch, lanza `TenantViolationException`.

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

---

## Ejecución con Docker

```bash
# Copiar variables de entorno
cp .env.example .env

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
./mvnw spring-boot:run
```

---

## Tests

```bash
# Ejecutar todos los tests
./mvnw test
```

### Cobertura de Tests

| Test                          | Tipo        | Qué verifica                                   |
|-------------------------------|-------------|------------------------------------------------|
| `AuthServiceTest`             | Unitario    | Registro, login, manejo de errores             |
| `CourseServiceTest`           | Unitario    | CRUD de cursos con tenant mock                 |
| `InstitutionServiceTest`      | Unitario    | Creación de instituciones, código duplicado    |
| `MultiTenancyIsolationTest`   | Integración | Aislamiento real entre tenants (H2)            |
| `JwtServiceTest`              | Unitario    | Emisión y parsing de tokens JWT                |
| `SecurityConfigTest`          | Integración | Endpoints públicos vs protegidos               |
| `TenantContextTest`           | Unitario    | ThreadLocal lifecycle, cross-tenant mode       |
| `HealthControllerTest`        | Integración | Health endpoint                                |
| `StreakStudyApplicationTests` | Integración | Carga del contexto Spring completo             |

---

## Equipo

Proyecto académico desarrollado para el curso **Desarrollo Basado en Plataformas (DBP)** — UTEC, 2026.
