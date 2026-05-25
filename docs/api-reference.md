> Parte de la documentación de [StreakStudy API](../README.md).

## API REST

Para facilitar la integración con el frontend y permitir pruebas rápidas de los endpoints, la plataforma cuenta con documentación interactiva basada en el estándar OpenAPI 3 y renderizada mediante Swagger UI.

### 1. Swagger UI (Acceso Local)
Cuando el servidor se encuentra ejecutándose localmente, se puede acceder a la interfaz gráfica interactiva a través de cualquiera de las siguientes rutas:
* http://localhost:8080/swagger-ui.html
* http://localhost:8080/swagger-ui/index.html

### 2. Especificación OpenAPI JSON
La definición técnica estructurada en formato JSON, útil para importar en herramientas externas o generar clientes de API automáticamente, se encuentra disponible en:
* http://localhost:8080/v3/api-docs

### 3. Flujo de Autenticación JWT en Swagger
Debido a que la mayoría de los endpoints del sistema requieren una sesión activa y la propagación del contexto Multi-Tenant, es necesario incluir el Bearer Token en Swagger para realizar peticiones exitosas:

1. Realiza una petición de registro o inicio de sesión desde los endpoints públicos de `/api/auth/register` o `/api/auth/login`.
2. Copia el token alfanumérico recibido en la propiedad `token` de la respuesta JSON.
3. En la parte superior derecha de la interfaz de Swagger UI, haz clic en el botón **Authorize**.
4. En el campo de texto, ingresa la palabra `Bearer` seguida de un espacio y pega tu token. Ejemplo: `Bearer eyJ...`
5. Haz clic en **Authorize** y cierra la ventana emergente. A partir de este momento, todos los requests incluirán el encabezado de seguridad de forma automática.

### 4. Características y Módulos Documentados
La interfaz agrupa y detalla los métodos HTTP, códigos de respuesta y esquemas de validación para los siguientes módulos core del sistema:
* **Auth:** Gestión de sesiones, registro de usuarios, renovación de tokens y recuperación de contraseñas.
* **Courses:** Administración de asignaturas específicas de cada institución educativa.
* **Decks:** Control de los mazos de tarjetas de estudio asignados a los estudiantes.
* **Flashcards:** Gestión individual de las preguntas y respuestas generadas de forma manual o mediante el asistente de IA.

---

## Detalle de Endpoints

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
| POST   | `/api/v1/auth/password/forgot`    | Solicita email con link de reset → 202     |
| POST   | `/api/v1/auth/password/reset`     | Confirma reset con token → 204             |

```json
POST /api/v1/auth/password/forgot
{
  "email": "alumno@utec.edu.pe"
}
```
**Respuesta:** `202 Accepted` siempre — existe o no el email (anti-enumeration). Si el correo está habilitado (`MAIL_ENABLED=true`), se envía el link a `${FRONTEND_URL}/reset-password?token=...`.

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
- **Rate limit** in-memory: 5 requests/email/hora a `/forgot` (configurable). Excedido → `429 Too Many Requests`.

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

