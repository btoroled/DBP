# Backlog Sprint Final — 10 Issues Spec-Driven

> Backlog versionado de los issues necesarios para cerrar todas las brechas
> de la **rúbrica CS 2031 (Semana 9/10)**. Cada sección es un issue completo
> y autocontenido, listo para copiarse a GitHub.
>
> **Punto de partida estimado:** ~15-16 / 20
> **Punto objetivo tras ejecutar los 9 core:** ~19-20 / 20
> **Esfuerzo total:** 5-7 días-persona

---

## Tabla de seguimiento

| # | Issue | Rúbrica | Pts en juego | Esfuerzo | Estado |
|---|---|---|---|---|---|
| 1 | ~~Sistema de eventos + email~~ | 8.1 + 8.3 | ~1.5 | 1-2 días | ✅ mergeado [#43](https://github.com/btoroled/DBP/pull/43) |
| 2 | Postman Collection v1 | Entregable S10 | obligatorio | 4h | ✅ implementado |
| 3 | ~~GitHub Actions CI~~ | §10 + bonus | ~0.3 | 3h | ✅ mergeado [#40](https://github.com/btoroled/DBP/pull/40) |
| 4 | ~~API hardening (/v1, CORS, path, HMNR)~~ | 5.2, 6.1, 7.1 | ~0.6 | 4h | ✅ mergeado [#43](https://github.com/btoroled/DBP/pull/43) |
| 5 | Swagger/OpenAPI | Bonus | ~0.3 | 2h | _por crear_ |
| 6 | ~~Refresh tokens + UserDetailsService~~ | 6.2 | ~0.3 | 4h | ✅ mergeado [#44](https://github.com/btoroled/DBP/pull/44) |
| 7 | @PreAuthorize + validaciones DTO | 1.3, 6.3 | ~0.5 | 4h | _por crear_ |
| 8 | Tests faltantes (Deck, Flashcard, Doc, Job) | 4.1, 4.2, 4.4 | ~0.8 | 1 día | _por crear_ |
| 9 | Logging SLF4J + cleanup + README final | 10.1 + bonus | ~0.2 | 2h | _por crear_ |
| 10 | ~~Password reset por email~~ | Extensión §6.2 + §8.3 | ~0.2 (bonus) | 4-6h | ✅ implementado (branch `issue-5-password-reset-email`, PR pendiente) |

**Orden de mergeo sugerido:** ~~#3~~ → ~~#4~~ → ~~#6~~ → ~~#1~~ → ~~#10~~ → #7 → #5 → #8 → ~~#2~~ → #9

---

## Issue 1 — feat: Sistema de eventos asíncronos + servicio de email transaccional

> **Spec-driven** · Cubre rúbrica §8.1 (Eventos), §8.3 (Email) y refuerza §8.2 (Asincronía).

### 1. Context
StreakStudy hoy no tiene `ApplicationEvent`s ni servicio de correo. La rúbrica exige eventos en **más de 2 casos de uso** (§8.1, 1.0 pto) y servicio de email con plantillas (§8.3, 0.5 pto). Sin esto perdemos ~1.5 pts.

### 2. Problem
Side-effects acoplados a lógica de negocio en `AuthService.register`, `DocumentProcessingService.generateFlashcards` y `StoreService.buyBadge`. Sin canal para notificar al usuario.

### 3. Goals
- **G1** Eventos: `UserRegisteredEvent`, `FlashcardsGeneratedEvent`, `BadgeEarnedEvent`.
- **G2** Listeners con `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("emailExecutor")`.
- **G3** `EmailSenderPort` + `JavaMailEmailSenderAdapter` con plantillas Thymeleaf HTML.
- **G4** Modo `MAIL_ENABLED=false` para CI/tests (log-only).
- **G5** Tests con `@RecordApplicationEvents` y GreenMail.

### 4. Non-goals
- i18n de plantillas, retries SMTP, cola persistente, tracking de aperturas.

### 5. Specification

#### 5.1 Paquetes nuevos
```
application/event/{UserRegisteredEvent,FlashcardsGeneratedEvent,BadgeEarnedEvent}.java
application/port/EmailSenderPort.java
infrastructure/email/{JavaMailEmailSenderAdapter,EmailProperties,EmailTemplateRenderer}.java
infrastructure/event/listener/{Welcome,FlashcardsReady,BadgeEarned}EmailListener.java
resources/templates/email/{welcome,flashcards-ready,badge-earned}.html
```

#### 5.2 Dependencias (pom.xml)
```xml
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
<dependency><groupId>com.icegreen</groupId><artifactId>greenmail-junit5</artifactId><version>2.0.1</version><scope>test</scope></dependency>
```

#### 5.3 ThreadPoolTaskExecutor en `AsyncConfig`
```java
@Bean("emailExecutor")
public Executor emailExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(2); exec.setMaxPoolSize(4); exec.setQueueCapacity(100);
    exec.setThreadNamePrefix("email-"); exec.initialize();
    return exec;
}
```

#### 5.4 Configuración SMTP
```properties
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USER:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=${MAIL_FROM:no-reply@streakstudy.com}
app.mail.enabled=${MAIL_ENABLED:false}
```

### 6. Acceptance Criteria
- **AC1** `POST /api/v1/auth/register` → 201 + email "Bienvenido a StreakStudy".
- **AC2** Email duplicado → rollback → **no** se envía email.
- **AC3** Job completed → email "Tus N flashcards están listas" al uploader.
- **AC4** Compra de badge → email "¡Ganaste el badge X!".
- **AC5** SMTP caído → log ERROR + no propaga; el request original devuelve 2xx.
- **AC6** Thread name del listener comienza con `email-` (asincronía verificada).
- **AC7** `MAIL_ENABLED=false` → log `[MAIL-DISABLED]` sin conexión SMTP.

### 7. Test Plan
- Unit: `WelcomeEmailListenerTest`, `JavaMailEmailSenderAdapterTest`, `EmailTemplateRendererTest`.
- Event: `@RecordApplicationEvents` en `AuthServiceEventTest`, `DocumentProcessingServiceEventTest`, `StoreServiceEventTest`.
- E2E: `EmailIntegrationTest` con GreenMail (puerto random, `withPerMethodLifecycle(true)`).
- Nomenclatura `shouldXxxWhenYyy`.

### 8. Rollout
1. Merge con `MAIL_ENABLED=false` por defecto en `.env.example`.
2. CI: `MAIL_ENABLED=false`.
3. Documentar vars en README (sección "Eventos y Email").

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| SMTP cae y bloquea threads | Executor dedicado + timeout SMTP <10s |
| `TenantContext` se pierde en `@Async` | El evento ya carga `institutionId`; listener no consulta repos |
| Tests flaky con GreenMail | Puerto random + per-method lifecycle |

### 10. Definition of Done
- [ ] 3 records de evento publicados desde `AuthService`/`DocumentProcessingService`/`StoreService`
- [ ] `EmailSenderPort` + adapter + 3 plantillas Thymeleaf
- [ ] `emailExecutor` configurado
- [ ] Tests unit + event + GreenMail verdes
- [ ] README actualizado con sección "Eventos y Email"
- [ ] Sin regresiones en suite existente

**Labels:** `feature` `events` `email` `rubrica-8.1` `rubrica-8.3`
**Estimación:** 1-2 días

---

## Issue 2 — docs: Postman Collection v1 con flujos completos y variables (Semana 10)

> **Spec-driven** · Entregable obligatorio Semana 10.

### 1. Context
La rúbrica de Semana 10 exige un archivo `postman_collection.json` en la raíz con todos los endpoints, variables y autorización configurada. Hoy no existe.

### 2. Problem
Sin colección Postman no se puede defender la API en la entrega ni demostrar los flujos al evaluador.

### 3. Goals
- **G1** Archivo `postman_collection.json` en raíz, importable con un click.
- **G2** Environment `streakstudy.postman_environment.json` con variables: `baseUrl`, `accessToken`, `institutionId`, `userId`, `deckId`, `flashcardId`.
- **G3** Autorización Bearer Token a nivel de colección con `{{accessToken}}`.
- **G4** Scripts post-response que guarden `accessToken` automáticamente tras login/register.
- **G5** Ejemplos guardados (Postman "Examples") para happy path y error 4xx por endpoint.
- **G6** Flujo end-to-end documentado como **Postman Runner**: register → login → create course → create deck → create flashcard → finish review → leaderboard.

### 4. Non-goals
- Tests de carga (Newman load).
- Mocks de Postman.
- CI con Newman (issue separado si se quiere).

### 5. Specification

#### 5.1 Estructura de carpetas dentro de la colección
```
StreakStudy API v1
├── 00 — Health
│   └── GET /api/v1/health
├── 01 — Auth
│   ├── POST /register (script: guarda accessToken)
│   ├── POST /login (script: guarda accessToken)
│   └── GET /me
├── 02 — Institutions
│   ├── POST /institutions
│   └── GET /institutions/{id}
├── 03 — Courses (CRUD)
├── 04 — Decks (CRUD)
├── 05 — Flashcards (CRUD)
├── 06 — Documents (upload PDF + status + flashcards job)
├── 07 — User Progress
│   ├── POST /users/me/progress/review
│   └── GET /users/me/progress
├── 08 — Leaderboard
├── 09 — Store
│   ├── POST /store/streak-freeze
│   └── POST /store/badges
└── 10 — Errors (ejemplos 400/401/403/404/409)
```

#### 5.2 Variables del environment
| Variable | Valor inicial | Uso |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Prefijo de URLs |
| `accessToken` | (vacío, lo llena el script) | Bearer |
| `institutionId` | `1` | Foreign key en register |
| `userId` | (vacío, lo llena el script) | — |
| `deckId` | (vacío, lo llena el script) | Cadena de tests |

#### 5.3 Script de ejemplo (post-response en login)
```javascript
pm.test("status 200", () => pm.response.to.have.status(200));
const body = pm.response.json();
pm.environment.set("accessToken", body.accessToken);
pm.environment.set("userId", body.user.id);
```

### 6. Acceptance Criteria
- **AC1** Importar colección + environment en Postman → no hay errores de variables sin resolver.
- **AC2** Correr "00→09" en Runner con DB vacía → todos los pasos verdes.
- **AC3** Cada endpoint tiene al menos 1 "Saved Example" de respuesta exitosa y 1 de error.
- **AC4** El Bearer se renueva automáticamente al re-ejecutar el endpoint de login.
- **AC5** La colección se referencia desde el README (`## API Documentation`).

### 7. Test Plan
- Ejecutar `Runner` localmente contra `docker compose up` limpio.
- Validar que las **15 respuestas guardadas** se renderizan en el visor.
- Pedir a 1 compañero importar y correr el Runner como prueba de portabilidad.

### 8. Rollout
1. Iterar la colección a medida que cambia la API.
2. Una vez mergeado el issue API hardening (`/api/v1`), regenerar URLs.
3. Subir al PR un screenshot del Runner verde.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| URLs `/api/...` cambian a `/api/v1/...` antes del merge | Mergear este issue **después** del de API hardening |
| Token expirado durante Runner | Renovar `accessToken` en pre-request script del folder protegido |
| Datos residuales rompen el flow | Usar emails con timestamp (`test+{{$timestamp}}@x.com`) |

### 10. Definition of Done
- [ ] `postman_collection.json` y `streakstudy.postman_environment.json` en raíz
- [ ] Runner verde end-to-end
- [ ] README enlaza colección con badge "Run in Postman"
- [ ] Screenshot del Runner adjunto al PR

**Labels:** `docs` `postman` `entregable-s10`
**Estimación:** 4h

---

## Issue 3 — ci: Workflow de GitHub Actions con build, test, cobertura JaCoCo y badges

> **Spec-driven** · Cubre rúbrica §10.2 (GitHub) + bonus CI/CD.

### 1. Context
No hay `.github/workflows/`. La rúbrica §10 evalúa el uso de GitHub Actions y la sección "GitHub & Management" del informe queda sin material.

### 2. Problem
Los PRs se mergean sin verificación automática. No hay evidencia de que `mvn verify` pase en una máquina limpia. No hay reporte de cobertura.

### 3. Goals
- **G1** Workflow `ci.yml` que corre en cada push y pull_request a `main`/`develop`/feature branches.
- **G2** Steps: checkout, setup-java 21, cache Maven, `mvn -B verify`, upload JaCoCo report.
- **G3** PostgreSQL service container (para tests de integración no-TestContainers).
- **G4** Badge de status en README.
- **G5** Branch protection rule sugerida: PRs a `main` requieren CI verde.

### 4. Non-goals
- Deployment automático (issue separado).
- Análisis de seguridad (Snyk, CodeQL) — bonus futuro.
- Newman/Postman CI.
- Matrix testing en múltiples JDKs.

### 5. Specification

#### 5.1 `.github/workflows/ci.yml`
```yaml
name: CI
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: streakstudy_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports: ['5432:5432']
        options: >-
          --health-cmd pg_isready --health-interval 10s
          --health-timeout 5s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Build and test
        env:
          DB_URL: jdbc:postgresql://localhost:5432/streakstudy_test
          DB_USER: postgres
          DB_PASSWORD: postgres
          JWT_SECRET: ci-secret-key-with-more-than-256-bits-length-padding-12345
          MAIL_ENABLED: 'false'
          ANTHROPIC_API_KEY: ''
        run: ./mvnw -B verify
      - name: Upload JaCoCo report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: target/site/jacoco
```

#### 5.2 Plugin JaCoCo en pom.xml
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution><goals><goal>prepare-agent</goal></goals></execution>
    <execution>
      <id>report</id><phase>verify</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

#### 5.3 Badge en README
```md
![CI](https://github.com/btoroled/DBP/actions/workflows/ci.yml/badge.svg)
```

### 6. Acceptance Criteria
- **AC1** Abrir un PR → check "CI" aparece en la lista de checks.
- **AC2** PR con código que falla un test → check rojo.
- **AC3** Tras merge a `main` → badge verde en README.
- **AC4** Artifact `jacoco-report` descargable desde el run.
- **AC5** El workflow corre en <10 minutos.

### 7. Test Plan
- PR con commit trivial → verificar trigger.
- PR con test roto a propósito → verificar fallo.
- Inspeccionar el report JaCoCo descargado (cobertura > 60% mínima para empezar).

### 8. Rollout
1. Mergear este issue primero, así los siguientes PRs ya validan en CI.
2. Configurar branch protection en `main` tras 2-3 PRs verdes consecutivos.
3. Informar al equipo: "ya no se mergea sin CI verde".

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| TestContainers no arranca en Actions | Usar el `services.postgres` y deshabilitar TestContainers vía profile `ci` o `@DisabledIfEnvironmentVariable("CI")` |
| Tiempos largos por dependencias | `actions/cache` con clave `~/.m2` |
| Tests dependen de SMTP real | `MAIL_ENABLED=false` |

### 10. Definition of Done
- [ ] `ci.yml` mergeado y corriendo
- [ ] Plugin JaCoCo activo
- [ ] Badge en README
- [ ] 1 PR validado end-to-end
- [ ] Sección "GitHub Actions" en README explicando el flujo

**Labels:** `ci` `tooling` `rubrica-10`
**Estimación:** 3h

---

## Issue 4 — refactor: API a /api/v1, CORS, path en ErrorResponse y HttpMessageNotReadable

> **Spec-driven** · Cubre rúbrica §5.2 (GlobalExceptionHandler), §6.1 (CORS), §7.1 (RESTful).

### 1. Context
Las rutas hoy son `/api/...`, no `/api/v1/...` como pide la rúbrica §7.1. `GlobalExceptionHandler` no incluye el campo `path` ni maneja `HttpMessageNotReadableException`. `SecurityConfig` no tiene CORS, lo que rompe el frontend en local.

### 2. Problem
1. Sin `/v1` perdemos puntos en RESTful (§7.1) y dificultamos versionar la API.
2. Sin `path` en errores, el evaluador no puede correlacionar respuesta con request.
3. Sin manejo de JSON malformado, devolvemos 500 cuando debería ser 400.
4. Sin CORS, el frontend (`localhost:5173`) recibe error de preflight.

### 3. Goals
- **G1** Migrar todos los `@RequestMapping("/api/...")` a `/api/v1/...`.
- **G2** Añadir bean `CorsConfigurationSource` permitiendo orígenes del frontend (`localhost:5173`, `localhost:3000` y URL de deployment).
- **G3** Agregar `path` al body de respuesta del `GlobalExceptionHandler` extrayéndolo del `HttpServletRequest`.
- **G4** Manejar `HttpMessageNotReadableException` → 400 con código `malformed_json`.
- **G5** Manejar `AccessDeniedException` → 403 con código `forbidden`.
- **G6** Mantener compatibilidad: si conviene, dejar `/api/health` y `/actuator/health` como están.

### 4. Non-goals
- HATEOAS (§7.1 menciona "considerado" — bonus opcional).
- Pagination en endpoints (issue futuro).
- Deprecar `/api/...` con redirects (no necesario, este es el primer release).

### 5. Specification

#### 5.1 Cambios en controllers
| Antes | Después |
|---|---|
| `/api/auth` | `/api/v1/auth` |
| `/api/institutions` | `/api/v1/institutions` |
| `/api/courses` | `/api/v1/courses` |
| `/api/decks` | `/api/v1/decks` |
| `/api/flashcards` | `/api/v1/flashcards` |
| `/api/documents` | `/api/v1/documents` |
| `/api/leaderboard` | `/api/v1/leaderboard` |
| `/api/store` | `/api/v1/store` |
| `/api/users/me/progress` | `/api/v1/users/me/progress` |
| `/api/reward-items` | `/api/v1/reward-items` |

#### 5.2 CORS bean en `SecurityConfig`
```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
        "http://localhost:5173", "http://localhost:3000",
        System.getenv().getOrDefault("FRONTEND_URL", "https://streakstudy.example.com")));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
    cfg.setExposedHeaders(List.of("Authorization"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
}
```
+ `.cors(Customizer.withDefaults())` en la chain.

#### 5.3 ErrorResponse con `path`
Cada handler recibe `HttpServletRequest request` y agrega `map.put("path", request.getRequestURI())`. Crear helper:
```java
private Map<String,Object> baseBody(HttpStatus s, String code, String msg, HttpServletRequest req) {
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("timestamp", Instant.now().toString());
    m.put("status", s.value());
    m.put("error", code);
    m.put("message", msg);
    m.put("path", req.getRequestURI());
    return m;
}
```

#### 5.4 Nuevos handlers
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String,Object>> malformed(HttpMessageNotReadableException ex, HttpServletRequest req) {
    return body(HttpStatus.BAD_REQUEST, "malformed_json", "JSON malformado o tipo invalido", req);
}

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<Map<String,Object>> denied(AccessDeniedException ex, HttpServletRequest req) {
    return body(HttpStatus.FORBIDDEN, "forbidden", "No tiene permiso para esta operacion", req);
}
```

### 6. Acceptance Criteria
- **AC1** Todas las rutas existentes responden bajo `/api/v1/...`.
- **AC2** Body de error contiene los 5 campos: `timestamp`, `status`, `error`, `message`, `path`.
- **AC3** `POST /api/v1/courses` con `{"name": ` (JSON roto) → 400 + `error=malformed_json`.
- **AC4** Preflight `OPTIONS /api/v1/courses` con `Origin: http://localhost:5173` → 200 con headers `Access-Control-Allow-*`.
- **AC5** Acceso sin autoridad correcta → 403 + `error=forbidden` (no 500).
- **AC6** Tests existentes actualizados con `/api/v1`; los 29+ tests siguen verdes.

### 7. Test Plan
- Buscar y reemplazar `/api/` por `/api/v1/` en tests (mecánico).
- Nuevo test `GlobalExceptionHandlerTest.shouldReturn400WithPathWhenJsonIsMalformed`.
- Nuevo test `SecurityConfigCorsTest.shouldReturnCorsHeadersForPreflight`.
- Regenerar Postman collection (issue #2 depende).

### 8. Rollout
1. Hacer este PR antes que Postman (#2) y Swagger (#5).
2. Comunicar al equipo de frontend el cambio de prefijo.
3. Si el deploy ya está vivo, coordinar deploy + frontend en ventana corta.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Frontend rompe en producción | Variable `VITE_API_URL` apunta al nuevo `/api/v1` |
| Tests asumen `/api/` | Search & replace con grep |
| CORS demasiado permisivo en prod | `FRONTEND_URL` env var en deployment |

### 10. Definition of Done
- [ ] Todos los `@RequestMapping` migrados a `/api/v1`
- [ ] CORS bean configurado y test verde
- [ ] `path` en todas las respuestas de error (verificar con un test por handler)
- [ ] Handlers para `HttpMessageNotReadableException` y `AccessDeniedException`
- [ ] Suite de tests verde

**Labels:** `refactor` `api` `security` `rubrica-5.2` `rubrica-6.1` `rubrica-7.1`
**Estimación:** 4h

---

## Issue 5 — docs: Documentación OpenAPI/Swagger con springdoc

> **Spec-driven** · Bonus rúbrica + soporta entregable Postman.

### 1. Context
La API no tiene documentación interactiva. La rúbrica menciona Swagger/OpenAPI como bonus.

### 2. Problem
El evaluador depende del README + Postman para entender los endpoints. Sin docs en vivo, dificulta exploración.

### 3. Goals
- **G1** Añadir `springdoc-openapi-starter-webmvc-ui`.
- **G2** Endpoint `/swagger-ui.html` y `/v3/api-docs` accesibles sin autenticación.
- **G3** Configurar `OpenAPI` bean con metadata: título, versión, descripción, contacto.
- **G4** Configurar esquema de seguridad Bearer JWT para que el "Authorize" funcione.
- **G5** Anotar al menos los controllers críticos con `@Tag` y endpoints con `@Operation` + `@ApiResponse`.

### 4. Non-goals
- Documentar todos los DTOs uno a uno con `@Schema` exhaustivo (springdoc infiere lo básico).
- Versionar OpenAPI YAML como artifact.

### 5. Specification

#### 5.1 Dependencia
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

#### 5.2 `OpenApiConfig`
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(new Info().title("StreakStudy API")
                .version("v1")
                .description("Plataforma de aprendizaje gamificada con IA")
                .contact(new Contact().name("Equipo StreakStudy").email("team@streakstudy.com")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .components(new Components().addSecuritySchemes("bearer-jwt",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
```

#### 5.3 Whitelist en `SecurityConfig`
```java
.requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
```

#### 5.4 Anotaciones a aplicar (mínimo)
- `@Tag(name="Auth")` en `AuthController`, `@Tag(name="Courses")` en `CourseController`, etc.
- `@Operation(summary="Registrar usuario")` en endpoints clave.
- `@ApiResponses({@ApiResponse(responseCode="201"), @ApiResponse(responseCode="409", description="Email duplicado")})` en `register`.

### 6. Acceptance Criteria
- **AC1** `GET /swagger-ui.html` renderiza la UI sin token.
- **AC2** El botón "Authorize" acepta JWT y lo aplica a endpoints protegidos.
- **AC3** Los endpoints aparecen agrupados por tag.
- **AC4** Cada tag tiene al menos un `@Operation` con summary.
- **AC5** README enlaza `/swagger-ui.html` en la sección "API Documentation".

### 7. Test Plan
- Manual: arrancar app, abrir `/swagger-ui.html`, intentar `POST /auth/register` desde la UI.
- Test de integración: `GET /v3/api-docs` → 200 con JSON OpenAPI 3.

### 8. Rollout
- Mergear después de #4 (API hardening) para que los paths queden ya en `/v1`.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Choque con Spring Security | Whitelist explícita de `/swagger-ui/**` y `/v3/api-docs/**` |
| Swagger expone endpoints en producción | OK para esta entrega (académica). En real-world, restringir por perfil. |

### 10. Definition of Done
- [ ] Springdoc agregado
- [ ] `/swagger-ui.html` accesible y funcional
- [ ] "Authorize" funciona con JWT
- [ ] 4+ controllers anotados con `@Tag` + `@Operation`
- [ ] README actualizado

**Labels:** `docs` `bonus` `openapi`
**Estimación:** 2h

---

## Issue 6 — feat: Refresh tokens y UserDetailsService personalizado

> **Spec-driven** · Completa rúbrica §6.2 (JWT 1.5 pts full).

### 1. Context
La rúbrica §6.2 (1.5 pto) exige explícitamente: refresh tokens, `UserDetailsService` personalizado, validación de expiración. Hoy tenemos JWT básico pero faltan los dos primeros — quedamos en 1.2 pto en lugar de 1.5.

### 2. Problem
1. Los access tokens viven 1 hora; cuando expiran, el usuario debe re-loguearse con password.
2. Spring Security usa el principal manualmente; sin `UserDetailsService` no podemos delegar la carga del usuario al framework (necesario para `@PreAuthorize` avanzados con `hasRole`/dominio).

### 3. Goals
- **G1** Endpoint `POST /api/v1/auth/refresh` que recibe `refreshToken` y emite nuevo `accessToken`.
- **G2** Tabla `refresh_tokens` con campos: id, user_id, token_hash (SHA-256), expires_at, revoked, created_at.
- **G3** `RefreshTokenService` con: create, validate, revoke, rotate.
- **G4** `JwtUserDetailsService implements UserDetailsService` que carga `User` por email/id.
- **G5** Endpoint `POST /api/v1/auth/logout` que revoca el refresh token activo.
- **G6** Reducir access token TTL a 15 minutos; refresh TTL a 30 días.

### 4. Non-goals
- Sliding sessions complejas.
- Multi-device session management UI.
- Detección de robo de refresh token (reuse detection) — bonus futuro.

### 5. Specification

#### 5.1 Entidad `RefreshTokenJpa`
```java
@Entity @Table(name = "refresh_tokens",
  indexes = {@Index(name="ix_refresh_user", columnList="user_id"),
             @Index(name="ix_refresh_hash", columnList="token_hash", unique=true)})
public class RefreshTokenJpa { ... }
```
Campos: `id`, `userId`, `tokenHash` (60 char), `expiresAt`, `revoked`, `createdAt`.

#### 5.2 Endpoints
```http
POST /api/v1/auth/refresh
Body: { "refreshToken": "..." }
200: { "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }

POST /api/v1/auth/logout
Header: Authorization: Bearer <access>
Body: { "refreshToken": "..." }
204: No Content
```

#### 5.3 Cambios en `AuthService.login` y `register`
Devuelven `AuthResponse` con ambos tokens. El refresh se almacena hasheado en BD.

#### 5.4 `JwtUserDetailsService`
```java
@Service
public class JwtUserDetailsService implements UserDetailsService {
    public UserDetails loadUserByUsername(String email) {
        User u = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        return org.springframework.security.core.userdetails.User.withUsername(u.email())
            .password(u.passwordHash())
            .authorities(u.role().name())
            .build();
    }
}
```

#### 5.5 Rotación
`/refresh` siempre: revoca el refresh recibido + emite uno nuevo (rotación obligatoria).

### 6. Acceptance Criteria
- **AC1** `POST /auth/refresh` con refresh válido y no revocado → 200 con access nuevo.
- **AC2** `POST /auth/refresh` con refresh expirado → 401 `refresh_token_expired`.
- **AC3** `POST /auth/refresh` con refresh revocado (logout previo) → 401 `refresh_token_revoked`.
- **AC4** Cada refresh exitoso revoca el anterior (rotación).
- **AC5** `POST /auth/logout` revoca el refresh y futuros usos devuelven 401.
- **AC6** El secret de JWT se carga de env var (ya lo hace — verificar).
- **AC7** Access token vive 15 min; refresh vive 30 días — configurables vía env.

### 7. Test Plan
- `AuthServiceRefreshTest`: rotación, expiración, revocación.
- `RefreshTokenRepositoryTest` con `@DataJpaTest`.
- `AuthControllerTest`: 3 endpoints, AC1-AC5.
- `JwtUserDetailsServiceTest`: usuario existente / no existente.

### 8. Rollout
1. DDL automático con `ddl-auto=update` (o agregar Flyway script si decides migrar).
2. Comunicar al frontend: ahora deben almacenar `refreshToken` también y llamar `/refresh` antes de expiración.
3. Documentar en README.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Refresh tokens persisten para siempre si no se limpian | Job de limpieza nocturno (opcional, fuera de scope) |
| Hash collisions | SHA-256 con salt UUID es prácticamente cero |
| Rollback de BD por nueva tabla | Tabla nueva, sin impacto a tablas existentes |

### 10. Definition of Done
- [ ] Tabla `refresh_tokens` creada
- [ ] 3 endpoints (`/refresh`, `/logout`) + cambios en `/login`, `/register`
- [ ] `JwtUserDetailsService` registrado en SecurityConfig
- [ ] Tests AC1-AC7
- [ ] README + Postman + Swagger actualizados

**Labels:** `feature` `security` `auth` `rubrica-6.2`
**Estimación:** 4h

---

## Issue 7 — security: @PreAuthorize granular y validaciones en DTOs restantes

> **Spec-driven** · Completa rúbrica §1.3 (Validaciones) y §6.3 (Roles, full 1.0).

### 1. Context
- Solo 9 de 24 DTOs tienen validaciones (`@NotNull/@Email/@Size`). La rúbrica §1.3 exige validaciones a nivel aplicación.
- `@PreAuthorize` aparece en 4 lugares; endpoints administrativos (institutions, courses, decks) están sin protección de rol. La rúbrica §6.3 (1.0 pto full) pide "métodos sensibles" protegidos.

### 2. Problem
1. Datos malformados llegan a la capa de servicio.
2. Cualquier usuario autenticado puede crear/borrar instituciones.
3. STUDENT podría borrar courses si no se valida.

### 3. Goals
- **G1** Agregar `@Valid` + validaciones (`@NotBlank/@Size/@Email/@Min/@Max`) a los 15 DTOs sin cobertura.
- **G2** Definir matriz de roles y aplicar `@PreAuthorize` en cada controller administrativo.
- **G3** Agregar rol `ADMIN` (y opcionalmente `INSTRUCTOR`) si no existe ya en `UserRole`.
- **G4** Tests que verifiquen 403 para roles incorrectos.

### 4. Non-goals
- Permisos a nivel de recurso individual (ABAC) — solo RBAC simple.
- UI de gestión de roles.

### 5. Specification

#### 5.1 DTOs a validar
| DTO | Validaciones a agregar |
|---|---|
| `FinishReviewRequest` | `@NotNull` en `flashcardId`, `correct` |
| `GenerateFlashcardsRequest` | `@NotNull deckId`; `@Min(1) chunks` |
| `BadgePurchaseRequest` | `@NotBlank @Size(max=50) badgeName` |
| `CreateCourseRequest` | (ya tiene) — revisar |
| `UpdateDeckRequest` | `@NotBlank @Size(max=200) name` |
| `DocumentUploadResponse` | (response, sin validación) |
| ... (revisar los 15) | |

#### 5.2 Matriz de roles
| Endpoint | Roles permitidos |
|---|---|
| `POST /institutions` | `ADMIN` (en lugar de `permitAll` actual) |
| `DELETE /institutions/{id}` | `ADMIN` |
| `POST /courses`, `DELETE /courses/{id}` | `ADMIN`, `INSTRUCTOR` |
| `POST /decks`, `PUT/DELETE /decks/{id}` | `INSTRUCTOR`, `STUDENT` (propio) |
| `POST/PUT/DELETE /flashcards` | `INSTRUCTOR`, `STUDENT` |
| `POST /store/badges`, `POST /store/streak-freeze` | `STUDENT` |
| `POST /users/me/progress/review` | `STUDENT` |
| `GET /leaderboard` | autenticado (cualquiera) |

#### 5.3 Cambio en `UserRole` enum
```java
public enum UserRole { STUDENT, INSTRUCTOR, ADMIN }
```

#### 5.4 Aplicación
- A nivel **clase** del controller con `@PreAuthorize("hasAnyAuthority('ADMIN')")` cuando todo el controller comparte rol.
- A nivel **método** cuando varía por endpoint.

### 6. Acceptance Criteria
- **AC1** `POST /institutions` sin rol `ADMIN` → 403 `forbidden`.
- **AC2** `STUDENT` intentando `DELETE /api/v1/courses/{id}` → 403.
- **AC3** Request con body inválido (sin `@NotNull` violado) → 400 con array `errors` apuntando al campo.
- **AC4** Los 15 DTOs documentados tienen al menos una validación.
- **AC5** Tests parametrizados por rol: por cada endpoint admin, un test 200 con rol correcto y un test 403 con rol incorrecto.

### 7. Test Plan
- `CourseControllerAuthorizationTest`: matriz de roles vs endpoints.
- `InstitutionControllerAuthorizationTest`: mismo patrón.
- Tests de validación: por DTO, un test que envíe el campo inválido y verifique el `field` y `message` en la respuesta.

### 8. Rollout
1. Mergear **después** del issue #6 (refresh tokens) para no tocar `JwtService` dos veces.
2. Si hay un usuario ADMIN seed, agregarlo en `data.sql` o en un endpoint protegido para bootstrap.
3. Actualizar README con la matriz de roles.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Romper flujos en frontend al pasar de permitAll a ADMIN | Comunicar antes; quizá temporalmente mantener `POST /institutions` abierto con un comentario `// TODO restringir cuando exista UI admin` |
| Falta de usuarios ADMIN en producción | Seed inicial vía `CommandLineRunner` con email/password de env vars |

### 10. Definition of Done
- [ ] 15 DTOs con validaciones
- [ ] Matriz de roles implementada vía `@PreAuthorize`
- [ ] Rol `ADMIN` (+ `INSTRUCTOR`) agregado a `UserRole`
- [ ] Tests AC1-AC5 verdes
- [ ] README con matriz de roles
- [ ] `AccessDeniedException` produce 403 con `error=forbidden` (depende de #4)

**Labels:** `security` `validation` `rubrica-1.3` `rubrica-6.3`
**Estimación:** 4h

---

## Issue 8 — test: Cobertura de tests para Deck, Flashcard, Document y AiGenerationJob

> **Spec-driven** · Completa rúbrica §4.1 (Repositorios), §4.2 (Servicios), §4.4 (TestContainers).

### 1. Context
La suite cubre auth/course/institution/leaderboard/store/userProgress, pero faltan tests del núcleo académico (Deck, Flashcard) y de los repos de Document/AiGenerationJob. TestContainers solo se usa en 2 tests.

### 2. Problem
- Sin `DeckServiceTest` / `FlashcardServiceTest`, regresiones en el dominio académico pasan sin alarma.
- 4 de 8 repositorios no tienen test (`Deck`, `Flashcard`, `Document`, `AiGenerationJob`).
- TestContainers limitado → no cubrimos comportamiento específico de PostgreSQL (índices, full-text si llega a usarse, JSONB, etc.).

### 3. Goals
- **G1** `DeckServiceTest` con Mockito — todos los métodos públicos.
- **G2** `FlashcardServiceTest` con Mockito — todos los métodos públicos.
- **G3** `DeckRepositoryAdapterTest`, `FlashcardRepositoryAdapterTest`, `DocumentRepositoryAdapterTest`, `AiGenerationJobRepositoryAdapterTest` con `@DataJpaTest`.
- **G4** Migrar al menos 2 de estos repo tests a TestContainers PostgreSQL.
- **G5** `DeckControllerTest` con `@WebMvcTest` (no existe).
- **G6** Apuntar a cobertura JaCoCo ≥ 80% en `application/service/` y `infrastructure/persistence/adapter/`.

### 4. Non-goals
- Tests E2E con Selenium/Cypress.
- Mutation testing.
- Tests de carga.

### 5. Specification

#### 5.1 Tests a crear
```
src/test/java/com/streakstudy/
  application/service/
    DeckServiceTest.java                          # NUEVO
    FlashcardServiceTest.java                     # NUEVO
  infrastructure/persistence/
    DeckRepositoryAdapterTest.java                # NUEVO @DataJpaTest H2
    FlashcardRepositoryAdapterTest.java           # NUEVO @DataJpaTest H2
    DocumentRepositoryAdapterTest.java            # NUEVO @DataJpaTest H2
    AiGenerationJobRepositoryAdapterTest.java     # NUEVO @DataJpaTest H2
    DeckRepositoryAdapterPostgresContainerTest.java       # NUEVO TestContainers
    FlashcardRepositoryAdapterPostgresContainerTest.java  # NUEVO TestContainers
  infrastructure/web/
    DeckControllerTest.java                        # NUEVO @WebMvcTest
```

#### 5.2 Nomenclatura
Todos: `shouldXxxWhenYyy`. Ejemplos:
- `shouldReturnFlashcardListWhenDeckExists`
- `shouldThrowEntityNotFoundWhenUpdatingMissingFlashcard`
- `shouldFilterByCurrentTenantWhenListingDecks`
- `shouldPersistAndRetrieveDocumentByHashWhenUsingPostgres`

#### 5.3 Casos mínimos por test
**Service tests:**
- Happy path por método público
- Edge cases: not found, tenant violation
- Mockeo correcto del repository y `TenantContext`

**Repository tests:**
- CRUD (save, findById, deleteById)
- Queries personalizadas (findByDeckId, findByInstitutionId, etc.)
- Filtrado por tenant
- Constraints (unique, not null)

#### 5.4 TestContainers configurado
```java
@DataJpaTest @Testcontainers @ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(DeckRepositoryAdapter.class)
class DeckRepositoryAdapterPostgresContainerTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("streakstudy_test");
    // ... DynamicPropertySource
}
```

#### 5.5 JaCoCo umbrales (opcional pero recomendado)
```xml
<execution>
  <id>check</id><phase>verify</phase>
  <goals><goal>check</goal></goals>
  <configuration>
    <rules><rule>
      <element>BUNDLE</element>
      <limits><limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.70</minimum></limit></limits>
    </rule></rules>
  </configuration>
</execution>
```

### 6. Acceptance Criteria
- **AC1** Tests nuevos verdes en local (`./mvnw test`).
- **AC2** Cobertura global ≥ 70%; capa de service ≥ 80%.
- **AC3** Al menos 4 tests con TestContainers (suma con los 2 existentes = 6).
- **AC4** Todos los tests nuevos usan nomenclatura BDD.
- **AC5** Tests corren en GitHub Actions (depende de issue #3).

### 7. Test Plan
- Correr `./mvnw verify` y revisar `target/site/jacoco/index.html`.
- Cherry-pick: rompe deliberadamente `DeckService.create` y verifica que el test falla.

### 8. Rollout
1. Iterativo: un PR por tipo (services first, luego repos, luego controller).
2. Si el umbral JaCoCo falla, ajustar a un valor real (no aspiracional).

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| TestContainers no arranca en GH Actions | Service container postgres como fallback (issue #3) |
| Tests flaky por orden | `@DirtiesContext` o `@Transactional` por test |
| Umbral JaCoCo bloquea PRs sin tiempo | Ajustar gradualmente: 60% → 70% → 80% |

### 10. Definition of Done
- [ ] 7 test files nuevos creados
- [ ] `DeckControllerTest` con @WebMvcTest
- [ ] 2 tests adicionales con TestContainers
- [ ] Cobertura ≥ 70% (reporte JaCoCo en PR)
- [ ] Sin tests flaky en 3 corridas consecutivas de CI

**Labels:** `test` `quality` `rubrica-4.1` `rubrica-4.2` `rubrica-4.4`
**Estimación:** 1 día

---

## Issue 9 — chore: SLF4J logging, cleanup de TestStreakController y README de entrega

> **Spec-driven** · Bonus logging + rúbrica §10.1 (README) + calidad general.

### 1. Context
- `JwtAuthenticationFilter` usa `System.out.println` + `printStackTrace` para errores.
- `TestStreakController` parece de debug — no debería estar en producción.
- README necesita secciones nuevas (Eventos, Email, CI badge, link deployment, matriz de roles).

### 2. Problem
1. Logs no estructurados rompen el bonus "Logging SLF4J + Logback" de la rúbrica.
2. Endpoints de testing en producción son superficie de ataque.
3. README incompleto baja §10.1 a 0.3 en lugar de 0.4.

### 3. Goals
- **G1** Reemplazar todo `System.out.println` y `printStackTrace` por SLF4J (`@Slf4j` o `LoggerFactory`).
- **G2** Eliminar `TestStreakController` o restringirlo a perfil `dev` con `@Profile("dev")`.
- **G3** Configurar Logback con patrón estructurado y niveles por paquete en `logback-spring.xml`.
- **G4** Actualizar README con: badges (CI, license), sección "Eventos y Email", sección "GitHub Actions", sección "Roles y Permisos", link al deployment, link Swagger/Postman.
- **G5** Verificar que `application.properties` no tiene `spring.jpa.show-sql=true` por default (ruido en logs).

### 4. Non-goals
- ELK/Loki/observabilidad externa.
- Tracing distribuido (Sleuth/Zipkin).
- Logs JSON para producción (bonus opcional).

### 5. Specification

#### 5.1 Sustituciones de logging
| Archivo | Antes | Después |
|---|---|---|
| `JwtAuthenticationFilter.java` | `System.out.println("...")` + `printStackTrace()` | `log.warn("JWT auth failed: {}", ex.getMessage())` (sin stack en WARN, en DEBUG sí) |
| `DocumentProcessingService.java` (catch genérico) | nada (silencioso) | `log.error("PDF processing failed for doc={}", documentId, e)` |

#### 5.2 `logback-spring.xml`
```xml
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern></encoder>
  </appender>
  <logger name="com.streakstudy" level="DEBUG"/>
  <logger name="org.hibernate.SQL" level="WARN"/>
  <root level="INFO"><appender-ref ref="CONSOLE"/></root>
</configuration>
```

#### 5.3 `TestStreakController` decision
Opción A (recomendada): eliminar.
Opción B: anotar `@Profile("dev")` para que solo cargue en local.

#### 5.4 README — secciones a agregar
```md
## CI/CD
![CI](.../ci.yml/badge.svg) — corre en cada PR.

## Eventos y Email
Sistema de eventos basado en `ApplicationEvent` con listeners async (ver issue #1).

## Roles y Permisos
| Rol | Endpoints accesibles |
|---|---|
| STUDENT | review, store, leaderboard |
| INSTRUCTOR | + courses, decks, flashcards |
| ADMIN | + institutions |

## Deployment
- API: https://streakstudy.onrender.com (o el que corresponda)
- Swagger: https://streakstudy.onrender.com/swagger-ui.html
- Postman: [colección](./postman_collection.json)

## Equipo
- ... (verificar nombres)
```

### 6. Acceptance Criteria
- **AC1** `grep -r "System.out.println\|printStackTrace" src/main/` devuelve 0 resultados.
- **AC2** `TestStreakController` eliminado o con `@Profile("dev")`.
- **AC3** `logback-spring.xml` presente; logs en `INFO` por default, `DEBUG` para `com.streakstudy`.
- **AC4** README tiene ≥ 8 secciones listadas en G4.
- **AC5** Badges en README renderizan correctamente en GitHub.
- **AC6** Equipo y licencia documentados.

### 7. Test Plan
- Manual: arrancar app, generar registro fallido, verificar log estructurado en consola.
- `grep` automatizado en CI (script de pre-commit opcional).
- Renderizar README en GitHub y revisar visual.

### 8. Rollout
- Último issue en mergearse (depende de #1, #3, #4, #5, #6, #7 para que los links del README apunten a algo real).

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Logs de prod muy verbosos | `<logger name="com.streakstudy" level="INFO"/>` en profile prod |
| Eliminar `TestStreakController` rompe demo | Confirmar con equipo que no es usado |

### 10. Definition of Done
- [ ] 0 ocurrencias de `System.out.println` en `src/main/`
- [ ] `TestStreakController` resuelto (eliminar o `@Profile`)
- [ ] `logback-spring.xml` activo
- [ ] README final con 8+ secciones + badges + links
- [ ] PR final con checklist completa de los 9 issues del proyecto

**Labels:** `chore` `docs` `logging` `rubrica-10.1`
**Estimación:** 2h

---

## Issue 10 — feat: Recuperación de contraseña por email (forgot/reset)

> **Spec-driven** · Extiende §6.2 (Autenticación) y §8.3 (Email). Depende del Issue #1 (eventos + email).

### 1. Context
StreakStudy permite registro y login (`AuthService`), pero no hay flujo para
recuperar la contraseña cuando el usuario la olvida. El Issue #1 ya provee la
infraestructura de eventos + plantillas Thymeleaf + `EmailSenderPort`, por lo
que este issue se apoya en ese tooling para el canal de envío.

### 2. Problem
1. Si un estudiante olvida su contraseña, hoy la única opción es pedirle al
   administrador que lo borre/recree — fricción inaceptable.
2. Ningún endpoint actual permite cambiar contraseña sin conocer la previa.
3. Sin token de un solo uso almacenado en BD, un esquema basado solo en JWT
   sería vulnerable a replay.

### 3. Goals
- **G1** Endpoint `POST /api/v1/auth/password/forgot` que recibe `{email}` y
  responde **siempre 202** (no revelar si el email existe — anti-enumeration).
- **G2** Endpoint `POST /api/v1/auth/password/reset` que recibe
  `{token, newPassword}` y, si el token es válido y no expiró, rotea el
  `passwordHash`.
- **G3** Tabla `password_reset_tokens` con `token_hash` (SHA-256), `expires_at`,
  `used_at`, `user_id`. **Nunca** almacenar el token en claro.
- **G4** `PasswordResetRequestedEvent` publicado tras persistir el token; un
  listener async (`emailExecutor`) renderiza la plantilla
  `password-reset.html` con el link `${frontendUrl}/reset?token=...` y lo
  envía con `EmailSenderPort` (reusa Issue #1).
- **G5** Al solicitar un nuevo reset, **invalidar tokens previos** del mismo
  usuario que sigan activos (rotación). Tras usar un token, marcarlo
  `used_at = now()`.
- **G6** Rate limiting suave: máximo 5 requests a `/forgot` por email por hora
  (in-memory Caffeine cache o bucket por email). Sin esto, un atacante puede
  enviar spam SMTP.

### 4. Non-goals
- Recuperación por SMS / 2FA.
- "Magic link login" (login sin password).
- Notificación al usuario cuando el password fue cambiado (puede ser un
  follow-up: `PasswordChangedEvent` + email "Tu contraseña fue actualizada").
- UI/frontend del flujo (lo construye el equipo de frontend).

### 5. Specification

#### 5.1 Paquetes nuevos
```
application/dto/{ForgotPasswordRequest,ResetPasswordRequest}.java
application/event/PasswordResetRequestedEvent.java
domain/model/PasswordResetToken.java
domain/repository/PasswordResetTokenRepository.java
domain/exception/{InvalidPasswordResetTokenException,PasswordResetTokenExpiredException}.java
application/service/PasswordResetService.java
infrastructure/persistence/entity/PasswordResetTokenJpa.java
infrastructure/persistence/repository/PasswordResetTokenJpaRepository.java
infrastructure/persistence/adapter/PasswordResetTokenRepositoryAdapter.java
infrastructure/persistence/mapper/PasswordResetTokenMapper.java
infrastructure/event/listener/PasswordResetEmailListener.java
resources/templates/email/password-reset.html
```

#### 5.2 Entidad `PasswordResetTokenJpa`
```java
@Entity @Table(name = "password_reset_tokens",
  indexes = {
    @Index(name="ix_pwreset_user", columnList="user_id"),
    @Index(name="ix_pwreset_hash", columnList="token_hash", unique=true)
  })
public class PasswordResetTokenJpa { ... }
```
Campos: `id`, `userId`, `tokenHash` (char(64) hex), `expiresAt`, `usedAt`,
`createdAt`. **NO** almacena el plaintext.

#### 5.3 Generación del token
```java
String plain = UUID.randomUUID() + "-" + UUID.randomUUID(); // 72+ chars
String hash  = sha256Hex(plain);
// persistir hash; devolver plain al listener via evento
```

#### 5.4 Endpoints

```http
POST /api/v1/auth/password/forgot
Body: { "email": "alice@utec.edu" }
202: (sin body — respuesta genérica siempre)
```

```http
POST /api/v1/auth/password/reset
Body: { "token": "<plain>", "newPassword": "Nueva123!" }
204: No Content
400: { "error": "invalid_password_reset_token", ... }
410: { "error": "password_reset_token_expired", ... }
```

#### 5.5 `PasswordResetService.requestReset`
```java
@Transactional
public void requestReset(String email) {
    userRepository.findByEmail(email).ifPresent(user -> {
        tokens.invalidateAllActiveFor(user.id()); // rotación
        String plain = newPlainToken();
        String hash  = sha256Hex(plain);
        Instant exp  = Instant.now().plus(Duration.ofMinutes(30));
        tokens.save(new PasswordResetToken(null, user.id(), hash, exp, null, Instant.now()));
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
            user.id(), user.email(), user.fullName(), plain, exp));
    });
    // Nota: NO se loggea si el email no existe (anti-enumeration).
}
```

#### 5.6 `PasswordResetService.confirmReset`
```java
@Transactional
public void confirmReset(String plainToken, String newPassword) {
    String hash = sha256Hex(plainToken);
    PasswordResetToken t = tokens.findByTokenHash(hash)
        .orElseThrow(InvalidPasswordResetTokenException::new);
    if (t.usedAt() != null) throw new InvalidPasswordResetTokenException();
    if (t.expiresAt().isBefore(Instant.now())) throw new PasswordResetTokenExpiredException();

    User user = userRepository.findById(t.userId()).orElseThrow(InvalidPasswordResetTokenException::new);
    userRepository.save(user.withPasswordHash(passwordHasher.hash(newPassword)));
    tokens.markUsed(t.id(), Instant.now());
}
```

Nota: requiere agregar `User.withPasswordHash(String)` al dominio (factory
copy method, manteniendo la inmutabilidad del record/clase actual).

#### 5.7 `PasswordResetEmailListener`
```java
@Async("emailExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
    String link = frontendUrl + "/reset-password?token=" + event.plainToken();
    emailSender.sendHtml(event.email(), "Restablece tu contraseña",
        "password-reset", Map.of(
            "fullName", event.fullName(),
            "resetLink", link,
            "expiresInMinutes", 30));
}
```

#### 5.8 Plantilla `password-reset.html`
HTML simple con saludo personalizado, un CTA prominente con el link, el TTL
en minutos y una nota de seguridad: "si no fuiste tú, ignora este mensaje".

#### 5.9 Configuración
```properties
app.password-reset.token-ttl-minutes=${PASSWORD_RESET_TTL_MIN:30}
app.password-reset.frontend-url=${FRONTEND_URL:http://localhost:5173}
```

### 6. Acceptance Criteria

- **AC1** `POST /api/v1/auth/password/forgot` con email existente →
  **202** + email enviado con link válido.
- **AC2** `POST /api/v1/auth/password/forgot` con email **no** existente →
  **202** (idéntica respuesta, sin email enviado, sin log que revele
  inexistencia).
- **AC3** `POST /api/v1/auth/password/reset` con token válido + password
  válido → **204** + el usuario puede loguearse con la nueva password.
- **AC4** Token usado dos veces → segundo intento devuelve
  **400 `invalid_password_reset_token`**.
- **AC5** Token vencido (>30 min) → **410 `password_reset_token_expired`**.
- **AC6** Solicitar `/forgot` dos veces seguidas: el primer token queda
  inválido al emitirse el segundo (rotación).
- **AC7** El plain token **nunca** aparece en logs ni en BD; solo el
  SHA-256 en `token_hash`.
- **AC8** Rate limit: la sexta solicitud `/forgot` para el mismo email en
  una hora → **429 Too Many Requests** (sin enviar email).
- **AC9** Login con la password anterior tras un reset exitoso → **401
  `invalid_credentials`**.

### 7. Test Plan
- Unit: `PasswordResetServiceTest` (rotación, token usado, expiración,
  hash determinista, anti-enumeration).
- Unit: `PasswordResetEmailListenerTest` con `EmailSenderPort` mockeado.
- Repo: `PasswordResetTokenRepositoryAdapterTest` (@DataJpaTest H2).
- Web: `AuthControllerPasswordResetTest` (MockMvc): AC1-AC5, AC8, AC9.
- Event: `@RecordApplicationEvents` para verificar que `forgot` publica
  exactamente un `PasswordResetRequestedEvent` por email existente y **cero**
  por email inexistente.
- Integración: extensión opcional del `EmailIntegrationTest` (GreenMail) con
  el flujo password-reset end-to-end.

### 8. Rollout
1. Mergear **después** del Issue #1 (necesita `EmailSenderPort` + listeners async).
2. Coordinar con frontend: la ruta del CTA en el email debe coincidir con
   la ruta real del frontend (`/reset-password?token=...`).
3. Documentar las dos rutas nuevas en README + Postman + Swagger.
4. Si el deploy está vivo, activar `MAIL_ENABLED=true` y validar primero
   con un usuario interno antes de comunicar el feature.

### 9. Risks

| Riesgo | Mitigación |
|---|---|
| User enumeration vía latencia (existente vs no existente difieren en ms) | Llamar siempre a `passwordHasher.hash("dummy")` en branch "no existe" para nivelar timing |
| Tokens viven para siempre si no se limpian | Job nocturno (`@Scheduled`) que borra `used_at IS NOT NULL OR expires_at < now()` (opcional, fuera de scope) |
| Spam SMTP | Rate limit por email (AC8) + `MAIL_ENABLED=false` en CI |
| Leak del link por reenvío del email | TTL corto (30 min) + un solo uso |
| BCrypt cost alto bloquea el hilo HTTP en `forgot` | Mantener el hashing dummy en el mismo executor (sigue siendo síncrono dentro del request — OK con cost 10) |

### 10. Definition of Done
- [ ] Tabla `password_reset_tokens` creada
- [ ] 2 endpoints (`/forgot`, `/reset`) + DTOs validados
- [ ] `PasswordResetService` con rotación, expiración y un solo uso
- [ ] `PasswordResetRequestedEvent` + listener async + plantilla Thymeleaf
- [ ] Rate limiting in-memory
- [ ] Tests AC1-AC9
- [ ] README + Postman + Swagger actualizados

**Labels:** `feature` `security` `auth` `email` `rubrica-6.2` `rubrica-8.3`
**Estimación:** 4-6h
**Depends on:** Issue #1 (eventos + email)

---


**Mantener este archivo actualizado:** cuando se cierre un issue, taquearlo en la tabla con `~~tachado~~` y el link al PR mergeado.
