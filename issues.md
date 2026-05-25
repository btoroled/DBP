# Backlog Final — Pendientes para puntaje máximo

> Estado tras merge a `main` de Issues #1, #3, #4, #6, #7, #8, #9, #10.
>
> **Puntaje actual estimado:** ~18.3 / 20 (sin deploy, sin Postman)
> **Puntaje objetivo tras estos 4 issues:** 20 / 20
> **Esfuerzo total restante:** ~9-11h

---

## Tabla de seguimiento

| # | Issue | Rúbrica | Pts en juego | Esfuerzo | Estado | Prioridad |
|---|---|---|---|---|---|---|
| 2 | Postman Collection v1 | Entregable S10 | obligatorio | 4h | _por crear_ | 🔥 P0 |
| 5 | Swagger/OpenAPI | Bonus | ~0.3 | 2h | _por crear_ | P2 |
| 11 | Deployment a Render/Railway con DB Postgres | §9 | **1.0-2.0** | 2-4h | _por crear_ | 🔥 P0 |
| 12 | Recortar README a 1000-2000 palabras | §10.1 | protege ~0.1 | 1h | _por crear_ | P3 |

**Orden sugerido:** #11 (deploy en background) → #2 → #5 → #12

---

## Issue 2 — docs: Postman Collection v1 con flujos completos y variables (Semana 10)

> **Spec-driven** · Entregable obligatorio Semana 10.

### 1. Context
La rúbrica de Semana 10 exige un archivo `postman_collection.json` en la raíz con todos los endpoints, variables y autorización configurada. Hoy no existe.

### 2. Problem
Sin colección Postman no se puede defender la API en la entrega ni demostrar los flujos al evaluador.

### 3. Goals
- **G1** Archivo `postman_collection.json` en raíz, importable con un click.
- **G2** Environment `streakstudy.postman_environment.json` con variables: `baseUrl`, `accessToken`, `refreshToken`, `institutionId`, `userId`, `deckId`, `flashcardId`.
- **G3** Autorización Bearer Token a nivel de colección con `{{accessToken}}`.
- **G4** Scripts post-response que guarden `accessToken` y `refreshToken` automáticamente tras login/register/refresh.
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
│   └── GET /actuator/health
├── 01 — Auth
│   ├── POST /api/v1/auth/register (script: guarda accessToken + refreshToken)
│   ├── POST /api/v1/auth/login (script: guarda accessToken + refreshToken)
│   ├── POST /api/v1/auth/refresh (script: rota tokens)
│   ├── POST /api/v1/auth/logout
│   ├── POST /api/v1/auth/password/forgot
│   └── POST /api/v1/auth/password/reset
├── 02 — Institutions
│   ├── POST /api/v1/institutions
│   └── GET /api/v1/institutions/{id}
├── 03 — Courses (CRUD)
├── 04 — Decks (CRUD)
├── 05 — Flashcards (CRUD)
├── 06 — Documents (upload PDF + status + flashcards job)
├── 07 — User Progress
│   ├── POST /api/v1/users/me/progress/review
│   └── GET /api/v1/users/me/progress
├── 08 — Leaderboard
├── 09 — Store
│   ├── POST /api/v1/store/streak-freeze
│   └── POST /api/v1/store/badges
└── 10 — Errors (ejemplos 400/401/403/404/409)
```

#### 5.2 Variables del environment
| Variable | Valor inicial | Uso |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Prefijo de URLs |
| `accessToken` | (vacío, lo llena el script) | Bearer |
| `refreshToken` | (vacío, lo llena el script) | Para `/auth/refresh` |
| `institutionId` | `1` | Foreign key en register |
| `userId` | (vacío, lo llena el script) | — |
| `deckId` | (vacío, lo llena el script) | Cadena de tests |

#### 5.3 Script de ejemplo (post-response en login)
```javascript
pm.test("status 200", () => pm.response.to.have.status(200));
const body = pm.response.json();
pm.environment.set("accessToken", body.accessToken);
pm.environment.set("refreshToken", body.refreshToken);
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
2. Subir al PR un screenshot del Runner verde.
3. Si Issue #11 (deploy) está mergeado, agregar entorno `prod` apuntando a la URL pública.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
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
- Mergear después de #11 (deploy) para que la URL pública esté disponible en el `servers` del OpenAPI.

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

## Issue 11 — deploy: Backend en Render/Railway con PostgreSQL en la nube

> **Spec-driven** · Cubre rúbrica §9 (Deployment, hasta 2.0 pts). Sin deploy el tope es ~18.

### 1. Context
La rúbrica §9 da 1.0 pto por deploy en "Instant Deployment" (Render/Railway/Heroku) y 2.0 pto por AWS ECS/RDS. Hoy la API no está desplegada: solo corre en local con Docker Compose.

### 2. Problem
1. Sin URL pública el evaluador no puede ejercitar la API ni Postman contra producción.
2. La Semana 9 cierra con 18.1/20 como techo si no hay deploy.
3. README tiene placeholder `https://streakstudy.example.com` que rompe la credibilidad del informe.

### 3. Goals
- **G1** App desplegada en Render (recomendado por simplicidad) o Railway, accesible públicamente vía HTTPS.
- **G2** Base de datos PostgreSQL gestionada en el mismo provider (Render Postgres / Railway Postgres).
- **G3** Variables de entorno configuradas como secrets en el provider: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ANTHROPIC_API_KEY`, `MAIL_*`, `FRONTEND_URL`, `JPA_DDL=validate`.
- **G4** Health check del provider apuntando a `/actuator/health`.
- **G5** Build automático en cada push a `main` (auto-deploy on commit).
- **G6** README actualizado con URLs reales (reemplazar placeholders Deployment).

### 4. Non-goals
- AWS ECS/RDS (más complejo; opcional si hay tiempo extra para +1.0 pto).
- Custom domain.
- CDN o autoscaling.
- Blue-green deployment.

### 5. Specification

#### 5.1 Provider recomendado: Render

Razones:
- Free tier suficiente para demo académica.
- Postgres gestionado con 90 días gratis (ideal para defensa).
- Detecta automáticamente `Dockerfile`.
- Health checks nativos.

#### 5.2 Configuración del servicio web

| Setting | Valor |
|---|---|
| Type | Web Service |
| Runtime | Docker |
| Dockerfile path | `./Dockerfile` |
| Branch | `main` |
| Auto-deploy | Yes |
| Health check path | `/actuator/health` |
| Region | Oregon (más cercana, US-West) |

#### 5.3 Variables de entorno (secrets en Render Dashboard)

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://<render-pg-host>:5432/<db>` (auto-link a la DB) |
| `DB_USER` | desde la DB creada |
| `DB_PASSWORD` | secret del provider |
| `JWT_SECRET` | secret ≥ 256 bits, generado con `openssl rand -hex 64` |
| `ANTHROPIC_API_KEY` | tu API key real (no la del CI) |
| `MAIL_ENABLED` | `true` (o `false` si no quieres usar SMTP en demo) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | credenciales reales si `MAIL_ENABLED=true` |
| `FRONTEND_URL` | URL del frontend desplegado o `*` para demo |
| `JPA_DDL` | `validate` (no `update` en prod) |
| `SERVER_PORT` | `8080` (Render lo expone con `$PORT`) |

#### 5.4 Ajuste necesario al `Dockerfile` (verificar)

Render inyecta `PORT` como env var. Confirmar que `application.properties` ya usa `server.port=${SERVER_PORT:8080}` (✅ ya está). En Render setear `SERVER_PORT=${PORT}` o cambiar a `server.port=${PORT:8080}`.

#### 5.5 Base de datos

1. Crear PostgreSQL 16 en Render (Free tier).
2. Copiar el "Internal Database URL" → setear como `DB_URL` en el servicio web.
3. Primera deploy: `JPA_DDL=update` para crear tablas; luego cambiar a `validate`.

### 6. Acceptance Criteria
- **AC1** `GET https://<app>.onrender.com/actuator/health` → `{"status":"UP"}`.
- **AC2** `POST https://<app>.onrender.com/api/v1/auth/register` con body válido → 201 con `accessToken` y `refreshToken`.
- **AC3** Login + create deck + list decks funcionan end-to-end contra prod.
- **AC4** `JPA_DDL=validate` y la app arranca sin errores (schema ya creado).
- **AC5** Push a `main` dispara un build automático visible en Render Dashboard.
- **AC6** README sección "Deployment" tiene URL real (sin placeholder).

### 7. Test Plan
- Manual: importar Postman collection (Issue #2) con `baseUrl=https://<app>.onrender.com` y correr el Runner end-to-end.
- Verificar que los emails (si `MAIL_ENABLED=true`) llegan a una bandeja real.
- Smoke test: `curl https://<app>.onrender.com/actuator/health` desde otra red.

### 8. Rollout
1. Crear cuenta Render (si no existe).
2. Crear DB Postgres primero (toma ~2 min).
3. Crear Web Service apuntando al repo + branch `main`.
4. Configurar env vars; iniciar primer deploy con `JPA_DDL=update`.
5. Verificar AC1-AC2.
6. Cambiar `JPA_DDL=validate` y re-deploy.
7. Actualizar README con la URL real (commit + PR).
8. Compartir URL con el evaluador en la entrega.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Cold start de Render free tier (~30s) | Documentar en README; usar uptime ping externo si afecta la demo |
| `ANTHROPIC_API_KEY` filtrada en logs | Confirmar que `JavaMailEmailSenderAdapter` y AI clients no loguean secrets |
| `JPA_DDL=update` deja schema inconsistente entre deploys | Migrar a Flyway en follow-up (fuera de scope) |
| Cuota free de Postgres (90 días) | Documentar fecha de expiración; migrar a paid si pasa la entrega |

### 10. Definition of Done
- [ ] App desplegada y accesible vía HTTPS
- [ ] Postgres en la nube linkeado al servicio
- [ ] Env vars como secrets (no en código)
- [ ] Health check verde
- [ ] AC1-AC6 verificados
- [ ] README actualizado con URL real (PR follow-up)
- [ ] Screenshot del dashboard de Render adjunto al PR

**Labels:** `deployment` `rubrica-9` `bloqueante`
**Estimación:** 2-4h

---

## Issue 12 — docs: Recortar README a 1000-2000 palabras

> **Spec-driven** · Protege rúbrica §10.1 (0.4 pto) — el README actual tiene **4357 palabras**, 2x el límite superior.

### 1. Context
La rúbrica de Semana 10 exige README "claro y bien estructurado, con una extensión de entre 1000 y 2,000 palabras". Excederlo arriesga bajar §10.1 de 0.4 a 0.3.

### 2. Problem
README sobrecargado mezcla referencia técnica detallada con el informe ejecutivo. El evaluador necesita escanear, no leer un manual completo.

### 3. Goals
- **G1** README principal ≤ 2000 palabras enfocado en: descripción, stack, instalación, deploy, equipo, links.
- **G2** Documentación técnica detallada (entidades, eventos, arquitectura interna) movida a `docs/`.
- **G3** Mantener todos los badges, links a deploy/Postman/Swagger en el README principal.
- **G4** Tabla de contenidos del README actualizada.

### 4. Non-goals
- Reescribir contenido (solo extraer + condensar).
- Generar diagramas nuevos.

### 5. Specification

#### 5.1 Estructura objetivo del README

```
1. Header + badges (~50w)
2. Descripción General (~200w)
3. Stack Tecnológico (~150w)
4. Quick Start (Docker + Local) (~250w)
5. API REST resumen (~300w, link a Swagger/Postman para detalles)
6. Seguridad y Autenticación resumen (~200w)
7. Eventos resumen (~150w)
8. Testing resumen (~150w, link a /docs/testing.md para detalles)
9. Deployment (~150w con URL real tras #11)
10. CI/CD (~100w, link a /docs/ci.md para detalles)
11. Equipo (~50w)
12. Licencia (~50w)
─────────
TOTAL: ~1800w
```

#### 5.2 Archivos a crear en `docs/`

| Archivo | Contenido extraído |
|---|---|
| `docs/architecture.md` | Diagrama hexagonal completo + descripciones de capas (~700w actuales) |
| `docs/entities.md` | Modelo de dominio + ER (~600w) |
| `docs/events.md` | Detalle de eventos + listeners + tabla completa (~500w) |
| `docs/api-reference.md` | Tabla full de endpoints (deprecar tras #5 Swagger) |
| `docs/testing.md` | Detalle de cada suite + matriz cobertura |
| `docs/ci.md` | Detalle workflow, variables, artifacts |

#### 5.3 Cada extracto en `docs/` enlaza de vuelta al README

```md
> Parte de la documentación de [StreakStudy API](../README.md).
```

### 6. Acceptance Criteria
- **AC1** `wc -w README.md` devuelve un valor entre 1000 y 2000.
- **AC2** README mantiene secciones obligatorias de la rúbrica: portada, índice, intro, descripción, modelo de entidades (resumen con link al detalle), testing, seguridad, eventos, GitHub, conclusión, apéndices.
- **AC3** Todos los links internos (TOC + links a `docs/`) funcionan en GitHub.
- **AC4** Cada doc en `docs/` enlaza de vuelta al README.
- **AC5** Badges, link a deploy, link a Postman y link a Swagger siguen visibles en el README principal.

### 7. Test Plan
- `wc -w README.md` < 2000.
- Push a una rama de prueba y verificar render en GitHub: TOC clickeable, links a `docs/*.md` funcionan.
- Lectura crítica: ¿un evaluador en 5 min entiende qué hace el proyecto y cómo correrlo?

### 8. Rollout
- Hacer **último**, después de #2, #5, #11 (para que los links reales estén disponibles).
- Mergear en un PR pequeño y enfocado solo a docs.

### 9. Risks
| Riesgo | Mitigación |
|---|---|
| Romper links internos al mover contenido | Buscar links de `#anchor` actuales antes de mover y actualizar |
| Perder información valiosa | Mover ≠ borrar: todo va a `docs/` |
| Conflictos con PRs en vuelo | Hacer al final cuando #2/#5/#11 estén mergeados |

### 10. Definition of Done
- [ ] README entre 1000-2000 palabras
- [ ] Carpeta `docs/` creada con archivos extraídos
- [ ] Links de `docs/` → README funcionan en GitHub
- [ ] AC1-AC5 verificados

**Labels:** `docs` `rubrica-10.1`
**Estimación:** 1h

---

**Mantener este archivo actualizado:** cuando se cierre un issue, mover su entrada al historial (abajo) o eliminarla. Cuando todos los issues estén cerrados, este archivo se borra.

---

## Historial (mergeados)

| # | Issue | PR |
|---|---|---|
| 1 | Sistema de eventos asíncronos + email | [#43](https://github.com/btoroled/DBP/pull/43) |
| 3 | GitHub Actions CI + JaCoCo | [#40](https://github.com/btoroled/DBP/pull/40) |
| 4 | API hardening (/v1, CORS, path, HMNR) | [#43](https://github.com/btoroled/DBP/pull/43) |
| 6 | Refresh tokens + UserDetailsService | [#44](https://github.com/btoroled/DBP/pull/44) |
| 7 | @PreAuthorize granular + validaciones DTO | [#47](https://github.com/btoroled/DBP/pull/47) |
| 8 | Tests faltantes (Deck, Flashcard, Doc, Job) + TestContainers | [#48](https://github.com/btoroled/DBP/pull/48) |
| 9 | Logging SLF4J + cleanup + README final | [#49](https://github.com/btoroled/DBP/pull/49) |
| 10 | Recuperación de contraseña por email | [#46](https://github.com/btoroled/DBP/pull/46) |
