# Pruebas de KidCheck

Todas las pruebas usan datos sintéticos. No cargues datos reales de menores.

## 1. Backend: unitarias e integración

Requisitos: Java 17+, Maven 3.9+ y Docker (para MySQL de Testcontainers).

```bash
cd backend
mvn test
```

Qué cubre:

- `HashesTest`: SHA-256 en hexadecimal minúsculas de 64 caracteres.
- `TiempoTest`: conversión UTC → `America/Lima`, cambio de día y límites diarios.
- `ApiIntegrationTest`: MySQL 8.4 real en Testcontainers, Flyway desde cero,
  login JWT, reportes diario/mensual, CSV y roles, auditoría, justificación
  pública, configuración/secciones e idempotencia del lote de ingresos con
  `observacion`.

El test de integración se omite automáticamente si Docker no está disponible
(`@Testcontainers(disabledWithoutDocker = true)`). Para la entrega, ejecútalo con
Docker activo.

## 2. Panel web: E2E con Playwright

Con los servicios levantados:

```bash
docker compose up -d --build
cd web
npx playwright install chromium   # solo la primera vez
npm run test:e2e
```

Si el panel no está en `http://127.0.0.1:5173`:

```bash
E2E_BASE_URL=http://localhost:5173 npm run test:e2e
```

Los 11 casos cubren:

- login de admin, auxiliar y vigilante;
- navegación y visibilidad por rol;
- error con credenciales inválidas y cierre de sesión;
- reportes R01/R02, gráficos y descarga CSV;
- auditoría con filtro por entidad;
- configuración, secciones y calendario;
- justificación pública válida y token inexistente.

## 3. Carga con k6

El script está en `tests/k6/kidcheck.js`. Usa 20 req/s durante 30 s para
`GET /panel/dia` y 50 req/s durante 30 s para `POST /sync/ingresos`.

```bash
docker run --rm --network host \
  -v "$PWD/tests/k6:/scripts:ro" \
  grafana/k6 run /scripts/kidcheck.js
```

Variables útiles: `BASE_URL`, `PANEL_RPS`, `PANEL_DURATION`, `SYNC_RPS`,
`SYNC_DURATION`, `SYNC_START`.

Umbrales exigidos por el plan:

- panel: p95 < 200 ms;
- sync: p95 < 300 ms;
- errores HTTP: < 1 %.

Medición local del 2026-10-23 (Docker Compose, equipo de desarrollo):

| Escenario | p95 | Errores |
|---|---:|---:|
| `GET /panel/dia` (20/s) | 23.24 ms | 0 % |
| `POST /sync/ingresos` (50/s) | 20.75 ms | 0 % |

El escenario de sync usa una fecha futura (`2026-12-31`) y el mismo alumno de
prueba; después de la primera inserción las siguientes peticiones ejercitan el
camino idempotente. Si quieres dejar la base demo limpia:

```bash
docker exec -i kidcheck-db mysql -uroot -proot kidcheck < tests/k6/cleanup.sql
```

## 4. Android: prueba JVM del hash QR

```bash
cd android
./gradlew :app:testDebugUnitTest
```

`PorteriaHashTest` verifica que el hash SHA-256 de la app coincide con el del
backend y que se recortan espacios/saltos de línea antes de hashear.

## 5. Prueba manual de portería

1. Instala el APK debug en la tablet/emulador.
2. Configura `http://10.0.2.2:8080` (emulador) o `http://IP-DEL-PC:8080`.
3. Inicia sesión con `vigilante1` / `KidCheck2026!`.
4. Activa modo avión: el roster cacheado debe seguir disponible.
5. Escanea un QR o registra manualmente con motivo.
6. Repite el mismo alumno: la cola local debe marcar duplicado.
7. Desactiva modo avión y pulsa **Sincronizar**.
8. Verifica el ingreso en `/dashboard` (polling cada 5 s).
9. Sal del kiosco con PIN `1234`.

Si el emulador no tiene cámara estable, usa **Buscar alumno**; el flujo de cola y
sincronización es el mismo.

## 6. Checklist antes de la demo

- [ ] `docker compose ps` muestra `db`, `api` y `web` arriba.
- [ ] `http://localhost:8080/actuator/health` responde `UP`.
- [ ] `npm run test:e2e` termina con 11/11.
- [ ] `mvn test` termina con 13/13.
- [ ] `docker compose logs api` no muestra errores de Flyway ni de conexión.
- [ ] El APK está instalado y el servidor apunta al host correcto.
- [ ] WhatsApp está en modo simulado (`TWILIO_ENABLED=false`) o el Sandbox está
      configurado.
- [ ] Los datos mostrados son los sintéticos de la semilla.
