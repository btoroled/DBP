#!/usr/bin/env bash
# Smoke test de SETUP-001.
# Levanta el stack con docker compose, espera a que la API este saludable
# y verifica que la conexion a la base de datos esta operativa.
#
# Uso:
#   ./scripts/smoke-test.sh            # levanta, prueba, deja corriendo
#   ./scripts/smoke-test.sh --teardown # levanta, prueba y apaga al final

set -euo pipefail

API_PORT="${API_PORT:-8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-180}"
TEARDOWN="false"
[[ "${1:-}" == "--teardown" ]] && TEARDOWN="true"

cd "$(dirname "$0")/.."

echo "==> Levantando stack con docker compose..."
docker compose up -d --build

echo "==> Esperando a que la API responda en http://localhost:${API_PORT}/actuator/health ..."
elapsed=0
until curl -fsS "http://localhost:${API_PORT}/actuator/health" >/dev/null 2>&1; do
    if (( elapsed >= TIMEOUT_SECONDS )); then
        echo "ERROR: timeout esperando a la API (${TIMEOUT_SECONDS}s)."
        docker compose logs --tail=80 api
        exit 1
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    echo "    ...esperando (${elapsed}s)"
done

echo "==> /actuator/health responde:"
curl -fsS "http://localhost:${API_PORT}/actuator/health" | sed 's/^/    /'
echo

echo "==> /api/health responde:"
curl -fsS "http://localhost:${API_PORT}/api/health" | sed 's/^/    /'
echo

echo "==> Verificando conexion a la base de datos (pg_isready) ..."
docker compose exec -T postgres pg_isready -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-streakstudy_db}"

echo "==> Verificando que Hibernate creo el schema (lista de tablas en public):"
docker compose exec -T postgres psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-streakstudy_db}" -c "\dt"

echo
echo "✅ SMOKE TEST OK — proyecto levanta, API saludable, DB accesible."

if [[ "${TEARDOWN}" == "true" ]]; then
    echo "==> Apagando stack..."
    docker compose down -v
fi
