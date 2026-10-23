# KidCheck

Sistema de control de ingreso escolar por credencial QR, con panel web y app Android de portería.

> Proyecto académico. Todos los datos de la semilla son ficticios; nunca se deben cargar datos reales de menores.

## Estructura

```
kidcheck/
├─ backend/    API REST (Spring Boot 3.5, Java 17, MySQL 8, Flyway)
├─ web/        Panel (SvelteKit 2 + Svelte 5, SPA con BFF de cookies httpOnly)
├─ android/    App de portería (Kotlin + Compose) — S5
└─ docker-compose.yml
```

## Arranque rápido con Docker

```bash
docker compose up --build
```

> Si Docker responde `permission denied` al conectar con `/var/run/docker.sock`,
> agrega tu usuario al grupo `docker` y vuelve a iniciar sesión:
> `sudo usermod -aG docker $USER`.

- Panel: <http://localhost:5173>
- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>

Usuario demo: `admin1` · contraseña `KidCheck2026!`

## Arranque local (sin Docker)

Requisitos: Java 17+, Maven 3.9+, MySQL 8.0.16+ y Node 20+.

```bash
# 1. MySQL en localhost:3306 (usuario root / root por defecto)
# 2. API: Flyway crea el esquema y carga la semilla
cd backend
mvn spring-boot:run

# 3. Panel, en otra terminal
cd web
npm install
npm run dev            # http://localhost:5173
```

Si el backend está en otra URL: `API_URL=http://mi-api:8080 npm run dev`.

## Usuarios de la semilla

| Usuario | Rol | Contraseña |
|---|---|---|
| `vigilante1` | vigilante | `KidCheck2026!` |
| `admin1` | administrador | `KidCheck2026!` |
| `auxiliar1` | auxiliar | `KidCheck2026!` |
| `psicologo1` | psicólogo | `KidCheck2026!` |

## Flujo de la demo (S2 + S3)

1. Entrar al panel con `admin1`.
2. **Apoderados** → registrar un contacto familiar (teléfono con código de país).
3. **Estudiantes** → *Nuevo estudiante*: datos, sección y apoderado.
4. Al guardar se emite la credencial: se muestra el QR y se descarga en PNG.
5. En la lista se puede **reemitir** (revoca la anterior) o **revocar** la credencial.

### Registro de ingresos (S3)

- La app de portería (o curl) llama `POST /api/v1/ingresos` con `Idempotency-Key`.
- Para probar sin la app, primero pide el roster y registra un ingreso:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"usuario":"vigilante1","password":"KidCheck2026!","deviceUuid":"11111111-1111-1111-1111-111111111111"}' \
  | jq -r .accessToken)

curl -s localhost:8080/api/v1/sync/roster -H "Authorization: Bearer $TOKEN" | jq

curl -s -X POST localhost:8080/api/v1/ingresos \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -H 'Idempotency-Key: demo-0001' \
  -d '{"idEstudiante":3,"deviceUuid":"11111111-1111-1111-1111-111111111111","fechaHoraUtc":"2026-10-23T12:41:05Z","canal":"qr"}'
```

- `POST /api/v1/sync/ingresos` acepta lotes desde la cola offline y devuelve
  `CREADO | DUPLICADO | ERROR` por elemento.
- `GET /api/v1/panel/dia` alimenta el panel con polling cada 5 s.
- Las notificaciones se guardan en la tabla `notificacion` (outbox) y un worker
  las envía cada 30 s. Con `TWILIO_ENABLED=false` el envío se simula.
- Solo se notifica a apoderados con consentimiento `notificaciones` en estado
  `autorizado`; la semilla ya los incluye. WhatsApp es el canal principal y el
  correo el respaldo cuando no hay teléfono.
- El job RN-01 marca ausentes pasada la hora límite + tolerancia y genera el
  enlace público `/j/<token>` para que el apoderado justifique en 72 h.
- El job RN-03 genera alertas de reincidencia los viernes a las 16:00 (Lima).

> Para una demo sin jobs automáticos: `KIDCHECK_JOBS_ENABLED=false`.

## Variables de entorno

### API

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de MySQL |
| `DB_PORT` | `3306` | Puerto |
| `DB_NAME` | `kidcheck` | Base de datos |
| `DB_USER` | `root` | Usuario |
| `DB_PASSWORD` | `root` | Contraseña |
| `JWT_SECRET` | (dev) | Secreto HMAC, mínimo 32 bytes |
| `JWT_ACCESS_MINUTES` | `15` | Vida del access token |
| `JWT_REFRESH_HOURS` | `8` | Vida del refresh token |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Orígenes permitidos |
| `WEB_PUBLIC_URL` | `http://localhost:5173` | Base del enlace público de justificación |
| `KIDCHECK_JOBS_ENABLED` | `true` | Activa RN-01 y RN-03 |
| `NOTIF_INTERVALO_MS` | `30000` | Frecuencia del worker de notificaciones |
| `TWILIO_ENABLED` | `false` | `true` envía por Twilio; `false` simula |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | — | Credenciales de Twilio |
| `TWILIO_WHATSAPP_FROM` | `whatsapp:+14155238886` | Número del sandbox |

### Panel

| Variable | Default | Descripción |
|---|---|---|
| `API_URL` | `http://localhost:8080` | URL interna del backend (solo servidor) |
| `PORT` | `3000` (`vite dev`: 5173) | Puerto del servidor Node |
| `ORIGIN` | — | Origen público cuando se despliega detrás de un proxy |

En Docker también puedes cambiar los puertos publicados con `API_PORT`,
`WEB_PORT` y `DB_PORT` (ver `.env.example`).

## Decisiones clave

- **QR sin biometría**: token aleatorio de 128 bits; en la base solo se guarda `SHA-256(token)`. El QR contiene el token en claro y la app compara hashes offline.
- **BFF**: el navegador nunca ve el JWT. Las cookies `kc_access` y `kc_refresh` son `httpOnly` y el proxy de SvelteKit renueva la sesión de forma transparente.
- **Offline-first** (Android, S5): roster cacheado + cola Room + sincronización idempotente por `Idempotency-Key`.
- **Outbox** para notificaciones WhatsApp (S3).

El plan completo está en `.opencode/plan/kidcheck-plan.md`.
