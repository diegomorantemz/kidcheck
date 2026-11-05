# Hito 60 % — Pruebas del flujo QR y marcación

**Fecha:** 2026-11-05
**Commit de código probado:** `1757cbc` (`fix(backend): permitir ingreso tras ausencia de RN-01`)
**Versiones:** API `0.1.0-SNAPSHOT` · panel `0.1.0` · Android `0.1.0` (versionCode 1)
**Entorno:** macOS, Docker/Colima, MySQL 8.4, API Spring Boot 3.5.12, Java 17, emulador Pixel 7 (API 33). El celular físico SM-A065M no estaba conectado durante la ejecución.
**Datos:** exclusivamente sintéticos de la semilla (`V2__seed.sql`).

> Estado general: **CP-03 PASS automatizado**. CP-01, CP-02 y CP-04 tienen **PASS parcial** en la capa servidor/unidad; la ejecución en cámara y la cola offline del dispositivo quedaron **BLOQUEADAS** por el entorno (ver `BLOQ-01`). No se marca PASS lo que no se ejecutó.

---

## 1. Casos seleccionados

| ID | Caso | Requisito | Tipo | Resultado |
|---|---|---|---|---|
| CP-01 | QR válido → ingreso → asistencia en panel | RF-01, RF-02 | API real + MySQL | **PASS parcial** (servidor/BD) |
| CP-02 | Reintento y duplicado del mismo alumno/día | RF-03, RF-04 | API real + MySQL | **PASS parcial** (idempotencia) |
| CP-03 | Flujo QR automatizado e idempotente | RF-01, RF-02, RF-04 | JUnit + Testcontainers | **PASS** |
| CP-04 | Token inválido no coincide con credencial | RF-05 | JUnit Android | **PASS parcial** (hash) |

Los IDs `RF-01…RF-05` son propuestos para este informe; se pueden mapear a los de Sesión 1.

---

## 2. Ejecución de CP-01 — QR válido

**Datos usados**

| Dato | Valor |
|---|---|
| Estudiante | `3` — Camila Torres Vega, 6°B |
| Token de la credencial | `tok-camila-0031` |
| SHA-256 esperado | `ce3b12487fdab9c7ebf24dee46fbcb8cff0e111a4a9e1e238f2012a9b710b1b8` |
| Fecha/hora UTC | `2026-11-05T12:30:00Z` → 07:30 Lima |
| `clientId` | `c7e328cf-4625-40c0-af3a-694d9fb5f077` |
| `deviceUuid` | `11111111-1111-1111-1111-111111111111` |
| Canal | `qr` |

**Ejecución:** login `vigilante1`, `POST /api/v1/sync/ingresos` con el lote anterior y consulta SQL posterior.

**Resultado obtenido**

```json
{"resultados":[{"clientId":"c7e328cf-4625-40c0-af3a-694d9fb5f077",
"status":"CREADO","idIngreso":6,"horaLocal":"07:30","mensaje":"Ingreso registrado"}]}
```

```text
id_ingreso  id_estudiante  fecha_local  hora_local  canal  idempotency_key
6           3              2026-11-05   07:30:00    qr     c7e328cf-4625-40c0-af3a-694d9fb5f077

id_asistencia  id_estudiante  fecha       estado    minutos_tarde  origen   id_ingreso
155            3              2026-11-05  presente  0              escaner  6
```

**Veredicto:** **PASS parcial**. El servidor creó un solo ingreso y actualizó la asistencia del día a `presente` con `origen=escaner`. La captura real desde la cámara de la app no se pudo ejecutar (ver `BLOQ-01`).

**Evidencia:** [`evidence/cp01-api.txt`](evidence/cp01-api.txt)

---

## 3. Ejecución de CP-02 — Idempotencia y duplicado

**Pasos:** reenviar el mismo lote con el mismo `clientId`; luego enviar el mismo alumno/día con otro `clientId`.

**Resultado obtenido**

- Mismo `clientId`: `DUPLICADO`, `idIngreso=6`, `horaLocal=07:30`, sin nueva fila.
- El conteo de ingresos del alumno 3 para `2026-11-05` permaneció en **1**.
- La asistencia siguió siendo **una sola fila**.

**Veredicto:** **PASS parcial**. La idempotencia y el rechazo de duplicado del servidor están verificados. La cola offline local (modo avión → Room → sincronizar) no se pudo ejecutar en dispositivo (ver `BLOQ-01`); esa capa queda pendiente de re-prueba.

**Evidencia:** [`evidence/cp01-api.txt`](evidence/cp01-api.txt) (incluye primer envío, reintento y SQL).

---

## 4. Ejecución de CP-03 — Automatizada

**Prueba:** `ApiIntegrationTest#flujoQrMarcacionEsIdempotenteYNoDuplica`

**Qué valida**

1. El roster devuelve `tokenHash` de 64 caracteres y coincide con `Hashes.sha256Hex("tok-camila-0031")`.
2. Un ingreso con `canal=qr` responde `CREADO`.
3. En MySQL hay 1 ingreso y 1 asistencia con `origen=escaner`.
4. El reintento con el mismo `clientId` responde `DUPLICADO`.
5. Un segundo `clientId` para el mismo alumno/día responde `DUPLICADO`.
6. El conteo final sigue siendo 1.

**Resultado**

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Veredicto:** **PASS**.

**Evidencia:** [`evidence/cp03.txt`](evidence/cp03.txt)

---

## 5. Ejecución de CP-04 — Token inválido

**Prueba:** `PorteriaHashTest#un token invalido no coincide con ninguna credencial semilla`

Se calcularon los hashes de los tokens de la semilla y el hash de `tok-invalido-9999`:

```text
ce3b12487fdab9c7ebf24dee46fbcb8cff0e111a4a9e1e238f2012a9b710b1b8  tok-camila-0031
572885633590382138a90b8a10d5f0a9e698d28d057bbdad8811731281e38f13  tok-mateo-0022
0e4acebd3226ddc31ad97cebd8a0621dc61faf38568786a08f572be331c7a4e4  tok-invalido-9999
```

El hash inválido no está en el conjunto de credenciales, por lo que `porHash` devuelve `null` y la app muestra “Credencial no reconocida”.

**Veredicto:** **PASS parcial**. La lógica de rechazo por hash está verificada; la pantalla de cámara no se ejecutó (ver `BLOQ-01`).

**Evidencia:** [`evidence/cp04-hash.txt`](evidence/cp04-hash.txt)

---

## 6. Defecto encontrado y corrección

### DEF-01 — Alumno marcado ausente no podía registrar su ingreso

| Campo | Detalle |
|---|---|
| Detectado en | CP-01, primer intento del 2026-11-05 |
| Síntoma | `POST /sync/ingresos` devolvió `ERROR_INTERNO` |
| Causa raíz | `IngresoService` insertaba siempre una fila nueva en `asistencia_dia`; ya existía la fila `ausente` de RN-01 y se violaba `uq_asistencia_dia` |
| Log | `Duplicate entry '3-2026-11-05' for key 'asistencia_dia.uq_asistencia_dia'` |
| Severidad | Alta: un alumno que llega después del corte no podía marcar ingreso |
| Corrección | `IngresoService` busca la asistencia del día y la **actualiza**; si no existe, la crea |
| Commit | `1757cbc` |
| Prueba de regresión | `unAlumnoMarcadoAusentePuedeRegistrarSuIngresoDespues` |
| Re-prueba | **PASS**: `CREADO`, una sola asistencia, `presente`, `origen=escaner` |

**Evidencia:** [`evidence/def01-fail.txt`](evidence/def01-fail.txt) (transcripción del FAIL), [`evidence/def01-retest.txt`](evidence/def01-retest.txt), [`evidence/cp01-api.txt`](evidence/cp01-api.txt)

---

## 7. Suites ejecutadas

| Suite | Comando | Resultado |
|---|---|---|
| Backend (unitarias + integración) | `mvn test` | **15/15 PASS** |
| Android (JVM) | `./gradlew :app:testDebugUnitTest` | **4/4 PASS** |
| Panel E2E (Playwright) | `npx playwright test` | **11/11 PASS** |

Notas:
- Backend: 9 de `ApiIntegrationTest`, 2 de `HashesTest`, 4 de `TiempoTest`.
- Android: 4 de `PorteriaHashTest`.
- Playwright: auth, roles, logout, reportes, auditoría, configuración y justificación pública.

**Evidencias:** [`evidence/backend-tests.txt`](evidence/backend-tests.txt), [`evidence/android-tests.txt`](evidence/android-tests.txt), [`evidence/e2e.txt`](evidence/e2e.txt)

---

## 8. Trazabilidad

| Requisito | Caso | Ejecución | Resultado | Evidencia |
|---|---|---|---|---|
| RF-01 Credencial QR válida | CP-01 + CP-03 | API real + JUnit | PASS parcial / PASS | `cp01-api.txt`, `cp03.txt`, `qr/camila.png` |
| RF-02 Marcación y asistencia | CP-01 + CP-03 | API real + JUnit | PASS parcial / PASS | `cp01-api.txt`, `cp03.txt` |
| RF-03 Sincronización/offline | CP-02 | API real | PASS parcial (servidor) | `cp01-api.txt` |
| RF-04 Duplicados e idempotencia | CP-02 + CP-03 | API real + JUnit | PASS / PASS | `cp01-api.txt`, `cp03.txt` |
| RF-05 Token inválido | CP-04 | JUnit Android | PASS parcial (hash) | `cp04-hash.txt` |

---

## 9. Bloqueos y pendientes

### BLOQ-01 — Sin dispositivo estable para cámara/offline

- El celular físico no estaba conectado durante la ejecución.
- El emulador Pixel 7 presentó ANR del **sistema** (no solo de KidCheck): Google Play Services, teclado, launcher, teléfono y otros procesos. La app mostró el diálogo “KidCheck Portería isn't responding” y el comando `input` dejó de responder.
- Evidencia: [`evidence/emulador-anr.txt`](evidence/emulador-anr.txt) y [`evidence/emulador-anr.png`](evidence/emulador-anr.png).
- **No se oculta:** CP-01, CP-02 y CP-04 quedan como PASS parcial en la capa ejecutada; la UI de cámara/offline queda pendiente.

**Re-prueba pendiente (20 min):**

```bash
adb devices
adb reverse tcp:8080 tcp:8080
# En la app: servidor http://127.0.0.1:8080
# CP-01: escanear docs/testing/evidence/qr/camila.png
# CP-02: modo avión, escanear mateo.png, repetir, sincronizar
# CP-04: escanear invalido.png
```

Los QR de prueba están en [`evidence/qr/`](evidence/qr/):
- `camila.png` → `tok-camila-0031`
- `mateo.png` → `tok-mateo-0022`
- `invalido.png` → `tok-invalido-9999`

> Si el celular está en otra red, usar `adb reverse` y `http://127.0.0.1:8080`; no requiere WiFi compartido.

---

## 10. Checklist para el hito 60 %

- [x] Flujo servidor QR → ingreso → asistencia ejecutado contra MySQL real (CP-01).
- [x] Idempotencia y rechazo de duplicado verificados (CP-02/CP-03).
- [x] Prueba automatizada nueva implementada y en verde (CP-03).
- [x] Defecto real encontrado, corregido y re-probado (DEF-01).
- [x] Backend 15/15, Android 4/4 y E2E 11/11.
- [x] Trazabilidad requisito → prueba → ejecución → evidencia.
- [ ] Evidencia de cámara real y cola offline en dispositivo (BLOQ-01).
- [ ] WhatsApp real (modo simulado, fuera del núcleo).
- [ ] Bloqueo MDM del kiosco (fuera de alcance declarado).

**Justificación del 60 %:** el núcleo funcional (credencial QR, hash, registro de ingreso, asistencia, idempotencia y reporte) está implementado y probado en servidor y suites automatizadas, con un defecto real detectado y corregido. La evidencia de campo en cámara/offline queda pendiente por un bloqueo de entorno, no por una falla del flujo del servidor. Con el celular conectado, los tres casos restantes se completan con los QR y comandos de la sección 9.

---

## 11. Reproducción

```bash
# Backend (Testcontainers)
cd backend
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
mvn test

# Android
cd ../android
./gradlew :app:testDebugUnitTest

# Panel
cd ../web
E2E_BASE_URL=http://localhost:5173 npx playwright test

# CP-01/CP-02 contra la API real
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"usuario":"vigilante1","password":"KidCheck2026!"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])')

CID=$(uuidgen | tr 'A-Z' 'a-z')
curl -s -X POST localhost:8080/api/v1/sync/ingresos \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d "{\"deviceUuid\":\"11111111-1111-1111-1111-111111111111\",\"ingresos\":[{\"clientId\":\"$CID\",\"idEstudiante\":3,\"fechaHoraUtc\":\"2026-11-05T12:30:00Z\",\"canal\":\"qr\"}]}"
```
