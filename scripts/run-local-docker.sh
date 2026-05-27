#!/usr/bin/env bash
# Levanta la API + Postgres en Docker para probar la imagen "como en AWS"
# antes de hacer push. Usa una red bridge dedicada para que la API resuelva
# Postgres por nombre (streakstudy-pg), igual que hara en ECS/EKS.
set -euo pipefail

# ─── Config ─────────────────────────────────────────────────────────────
IMAGE_TAG="streakstudy-api:dev"
NETWORK="streakstudy-local-net"
PG_CONTAINER="streakstudy-pg"
API_CONTAINER="streakstudy-api"

DB_NAME="streakstudy_db"
DB_USER="postgres"
DB_PASSWORD="postgres"

JWT_SECRET="dev-only-secret-please-change-this-must-be-at-least-256-bits-long-xx"
ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-}"

# ─── Limpieza previa ────────────────────────────────────────────────────
echo "▶ Limpiando contenedores previos..."
docker rm -f "$API_CONTAINER" "$PG_CONTAINER" >/dev/null 2>&1 || true

# ─── Red ────────────────────────────────────────────────────────────────
docker network inspect "$NETWORK" >/dev/null 2>&1 \
  || docker network create "$NETWORK" >/dev/null

# ─── Build imagen ───────────────────────────────────────────────────────
echo "▶ Build de la imagen $IMAGE_TAG..."
DOCKER_BUILDKIT=1 docker build -t "$IMAGE_TAG" .

# ─── Postgres ───────────────────────────────────────────────────────────
echo "▶ Levantando Postgres..."
docker run -d \
  --name "$PG_CONTAINER" \
  --network "$NETWORK" \
  -e POSTGRES_DB="$DB_NAME" \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -p 5434:5432 \
  --health-cmd="pg_isready -U $DB_USER -d $DB_NAME" \
  --health-interval=3s \
  --health-retries=10 \
  postgres:16-alpine >/dev/null

echo "▶ Esperando a que Postgres este healthy..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' "$PG_CONTAINER" 2>/dev/null)" = "healthy" ]; do
  sleep 2
done
echo "  ✔ Postgres listo"

# ─── API ────────────────────────────────────────────────────────────────
echo "▶ Levantando API..."
docker run -d \
  --name "$API_CONTAINER" \
  --network "$NETWORK" \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=default \
  -e SERVER_PORT=8080 \
  -e DB_URL="jdbc:postgresql://$PG_CONTAINER:5432/$DB_NAME" \
  -e DB_USER="$DB_USER" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JPA_DDL=update \
  -e JPA_SHOW_SQL=false \
  -e JWT_SECRET="$JWT_SECRET" \
  -e JWT_EXPIRATION_MS=3600000 \
  -e JWT_REFRESH_EXPIRATION_MS=2592000000 \
  -e ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" \
  -e MAIL_ENABLED=false \
  -e MANAGEMENT_HEALTH_MAIL_ENABLED=false \
  -e MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always \
  -e FRONTEND_URL=http://localhost:5173 \
  "$IMAGE_TAG" >/dev/null

# ─── Esperar healthcheck ────────────────────────────────────────────────
echo "▶ Esperando a que la API este UP (puede tardar ~30-60s)..."
for i in {1..40}; do
  if curl -fsS http://localhost:8080/actuator/health/liveness >/dev/null 2>&1; then
    echo "  ✔ API responde"
    break
  fi
  sleep 3
  if [ "$i" = "40" ]; then
    echo "  ✘ La API no respondio. Logs:"
    docker logs --tail 80 "$API_CONTAINER"
    exit 1
  fi
done

# ─── Verificacion ───────────────────────────────────────────────────────
echo ""
echo "▶ /actuator/health/liveness:"
curl -fsS http://localhost:8080/actuator/health/liveness | sed 's/^/  /'
echo ""
echo "▶ /actuator/health (con detalles):"
curl -sS http://localhost:8080/actuator/health | sed 's/^/  /'
echo ""
echo ""
echo "Listo. Comandos utiles:"
echo "  docker logs -f $API_CONTAINER"
echo "  docker exec -it $PG_CONTAINER psql -U $DB_USER -d $DB_NAME"
echo "  docker rm -f $API_CONTAINER $PG_CONTAINER"
