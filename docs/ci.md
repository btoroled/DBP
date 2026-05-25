> Parte de la documentación de [StreakStudy API](../README.md).
## GitHub Actions (CI)

El repositorio incluye un pipeline de integración continua en `.github/workflows/ci.yml` que se ejecuta automáticamente en cada `push` y `pull_request` a las ramas `main` y `develop`.

### Flujo del workflow

1. **Checkout** del código (`actions/checkout@v4`).
2. **Setup JDK 21 Temurin** con cache de Maven (`actions/setup-java@v4`).
3. **Service container PostgreSQL 16** (alpine) levantado por el runner con healthcheck `pg_isready`. Se expone en `localhost:5432` y se inyecta vía las variables `DB_URL`, `DB_USER`, `DB_PASSWORD`.
4. **Build + tests + cobertura**: `./mvnw -B -ntp verify`. La fase `verify` dispara el plugin JaCoCo (`jacoco-maven-plugin` 0.8.12), que genera el reporte en `target/site/jacoco`.
5. **Artifacts**:
    - `jacoco-report` → HTML/XML de cobertura (`target/site/jacoco`).
    - `surefire-reports` → resultados detallados de cada test.

Ambos artifacts se suben con `if: always()` para tenerlos disponibles incluso si los tests fallan.

### Variables de entorno usadas en CI

| Variable             | Valor en CI                                                          |
|----------------------|-----------------------------------------------------------------------|
| `DB_URL`             | `jdbc:postgresql://localhost:5432/streakstudy_test`                  |
| `DB_USER`            | `postgres`                                                            |
| `DB_PASSWORD`        | `postgres`                                                            |
| `JWT_SECRET`         | clave dummy de >= 256 bits (no se usa en producción)                  |
| `MAIL_ENABLED`       | `false`                                                               |
| `ANTHROPIC_API_KEY`  | vacío (las llamadas a IA se ejercitan con mocks en tests)             |

### Cobertura de tests (JaCoCo)

Para generar el reporte de cobertura en local:

```bash
./mvnw verify
open target/site/jacoco/index.html   # macOS
```

El plugin está declarado en `pom.xml` con dos ejecuciones:

- `prepare-agent` → instrumenta los tests (binding por defecto a `initialize`).
- `report` → genera el HTML/XML en `verify`.

### Branch protection sugerida

Una vez verificados 2-3 PRs en verde, se recomienda activar en GitHub:

- **Settings → Branches → Branch protection rules → `main`**
    - Require status checks to pass before merging → marcar el check **CI / build-test**.
    - Require pull request reviews → al menos 1 reviewer.

Esto impide mergear código que no compile o que rompa tests.

### Descargar artifacts

En cada run (Actions → CI → run específico) aparecen los artifacts al final. El `jacoco-report` se puede descomprimir y abrir `index.html` para inspeccionar líneas cubiertas/no cubiertas por paquete.

---
