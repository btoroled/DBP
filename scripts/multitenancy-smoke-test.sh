#!/usr/bin/env bash
# Smoke test end-to-end de multi-tenancy (TENANT-001).
#
# Crea 2 instituciones, registra 1 usuario por cada una, crea cursos en cada
# tenant, y verifica que el usuario A solo ve cursos de A y el usuario B solo
# ve cursos de B.
#
# Requiere: la app corriendo en API_HOST (por defecto http://localhost:8080).
# Usalo despues de: docker compose up -d --build (o ./mvnw spring-boot:run).

set -euo pipefail

API="${API_HOST:-http://localhost:8080}"

j() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)"; }

echo "==> 1. Creando institucion A (utec) ..."
INST_A=$(curl -fsS -X POST "$API/api/institutions" \
    -H "Content-Type: application/json" \
    -d '{"name":"UTEC","code":"utec"}')
echo "    $INST_A"
INST_A_ID=$(echo "$INST_A" | j "['id']")

echo "==> 2. Creando institucion B (pucp) ..."
INST_B=$(curl -fsS -X POST "$API/api/institutions" \
    -H "Content-Type: application/json" \
    -d '{"name":"PUCP","code":"pucp"}')
echo "    $INST_B"
INST_B_ID=$(echo "$INST_B" | j "['id']")

echo "==> 3. Registrando usuario en A ..."
REG_A=$(curl -fsS -X POST "$API/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"institutionId\":$INST_A_ID,\"email\":\"alice@utec.edu\",\"password\":\"Password123\",\"fullName\":\"Alice\"}")
TOKEN_A=$(echo "$REG_A" | j "['token']")
echo "    token A: ${TOKEN_A:0:40}..."

echo "==> 4. Registrando usuario en B ..."
REG_B=$(curl -fsS -X POST "$API/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"institutionId\":$INST_B_ID,\"email\":\"bob@pucp.edu\",\"password\":\"Password123\",\"fullName\":\"Bob\"}")
TOKEN_B=$(echo "$REG_B" | j "['token']")
echo "    token B: ${TOKEN_B:0:40}..."

echo "==> 5. Alice (tenant A) crea 2 cursos en A ..."
curl -fsS -X POST "$API/api/courses" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN_A" \
    -d '{"name":"Calculo A","description":"Curso A1"}' >/dev/null
curl -fsS -X POST "$API/api/courses" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN_A" \
    -d '{"name":"Algebra A","description":"Curso A2"}' >/dev/null

echo "==> 6. Bob (tenant B) crea 1 curso en B ..."
curl -fsS -X POST "$API/api/courses" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN_B" \
    -d '{"name":"Calculo B","description":"Curso B1"}' >/dev/null

echo "==> 7. Alice lista cursos: deben aparecer SOLO los de A ..."
LIST_A=$(curl -fsS "$API/api/courses" -H "Authorization: Bearer $TOKEN_A")
echo "    $LIST_A"
COUNT_A=$(echo "$LIST_A" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
if [[ "$COUNT_A" != "2" ]]; then
    echo "    ❌ FAIL: esperaba 2 cursos para Alice, obtuve $COUNT_A"
    exit 1
fi
INST_IDS_A=$(echo "$LIST_A" | python3 -c "import sys,json; ids={c['institutionId'] for c in json.load(sys.stdin)}; print(ids)")
echo "    institutionIds devueltos: $INST_IDS_A"
if ! echo "$INST_IDS_A" | grep -q "$INST_A_ID"; then
    echo "    ❌ FAIL: cursos de Alice no estan en su tenant"
    exit 1
fi
if echo "$INST_IDS_A" | grep -q "$INST_B_ID"; then
    echo "    ❌ FUGA DE DATOS: Alice ve cursos del tenant B"
    exit 1
fi

echo "==> 8. Bob lista cursos: deben aparecer SOLO los de B ..."
LIST_B=$(curl -fsS "$API/api/courses" -H "Authorization: Bearer $TOKEN_B")
echo "    $LIST_B"
COUNT_B=$(echo "$LIST_B" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
if [[ "$COUNT_B" != "1" ]]; then
    echo "    ❌ FAIL: esperaba 1 curso para Bob, obtuve $COUNT_B"
    exit 1
fi

echo "==> 9. Verificando que Alice NO puede acceder a un curso de B por id ..."
COURSE_B_ID=$(echo "$LIST_B" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API/api/courses/$COURSE_B_ID" -H "Authorization: Bearer $TOKEN_A")
if [[ "$HTTP_CODE" != "404" ]]; then
    echo "    ❌ FAIL: esperaba 404, obtuve $HTTP_CODE (potencial fuga de datos)"
    exit 1
fi
echo "    OK: respuesta = $HTTP_CODE (Not Found, como debe ser)"

echo "==> 10. Sin token: GET /api/courses debe devolver 401 ..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API/api/courses")
if [[ "$HTTP_CODE" != "401" && "$HTTP_CODE" != "403" ]]; then
    echo "    ❌ FAIL: esperaba 401/403, obtuve $HTTP_CODE"
    exit 1
fi
echo "    OK: respuesta = $HTTP_CODE (no autorizado, como debe ser)"

echo
echo "✅ MULTI-TENANCY SMOKE TEST OK"
echo "   - Cada usuario solo ve datos de su institucion"
echo "   - Acceso cruzado por id devuelve 404"
echo "   - Endpoints autenticados rechazan requests sin token"
