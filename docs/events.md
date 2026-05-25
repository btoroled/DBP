> Parte de la documentación de [StreakStudy API](../README.md).
## Eventos y Email

StreakStudy desacopla los side-effects de la lógica de negocio publicando
`ApplicationEvent`s desde los servicios y consumiéndolos en listeners
asíncronos. El correo transaccional se envía en plantillas Thymeleaf desde
un thread pool dedicado (`emailExecutor`, 2–4 hilos).

### Eventos publicados

| Evento                       | Publicado por                                         | Trigger                                       |
|------------------------------|-------------------------------------------------------|-----------------------------------------------|
| `UserRegisteredEvent`        | `AuthService.register`                                | Registro exitoso (commit de transacción)      |
| `FlashcardsGeneratedEvent`   | `DocumentProcessingService.generateFlashcards`        | Job de IA completado                          |
| `BadgeEarnedEvent`           | `StoreService.buyBadge`                               | Compra de badge exitosa (commit de transacción) |

Los eventos son `record`s **self-contained**: cargan todos los datos que el
listener necesita (email, fullName, deckId, etc.) para que el listener no
tenga que consultar repositorios. Esto evita problemas con `TenantContext`
en threads asíncronos.

### Listeners de email

| Listener                          | Plantilla                       | Estrategia                                                   |
|-----------------------------------|---------------------------------|--------------------------------------------------------------|
| `WelcomeEmailListener`            | `welcome.html`                  | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`        |
| `FlashcardsReadyEmailListener`    | `flashcards-ready.html`         | `@EventListener` + `@Async` (corre tras job async)            |
| `BadgeEarnedEmailListener`        | `badge-earned.html`             | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`        |

`AFTER_COMMIT` garantiza que, si la transacción que publicó el evento hace
rollback (p.ej. email duplicado detectado por una unique constraint), el
correo nunca se envía. El thread pool `email-*` desacopla el envío del
request HTTP original.

### Modo `MAIL_ENABLED=false`

Por defecto la app arranca con `app.mail.enabled=false`: el adapter loguea
`[MAIL-DISABLED] to=... subject=...` sin contactar al servidor SMTP. Útil
para desarrollo local, CI y tests. Para enviar correo real:

```bash
MAIL_ENABLED=true \
MAIL_HOST=smtp.gmail.com \
MAIL_PORT=587 \
MAIL_USER=tu-cuenta@gmail.com \
MAIL_PASSWORD=tu-app-password \
MAIL_FROM=no-reply@streakstudy.com \
./mvnw spring-boot:run
```

Si el envío SMTP falla, el adapter loguea ERROR pero **no propaga la
excepción**: el flujo de negocio del request original no se rompe.

### Plantillas Thymeleaf

Ubicadas en `src/main/resources/templates/email/`:

- `welcome.html` — Bienvenida al registrarse
- `flashcards-ready.html` — Notificación de flashcards generadas
- `badge-earned.html` — Notificación de badge desbloqueado