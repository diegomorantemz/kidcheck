# KidCheck

Sistema de control de ingreso escolar por credencial QR, con panel web y app Android de portería.

> Proyecto académico. Todos los datos de la semilla son ficticios; nunca se deben cargar datos reales de menores.

## Estructura

```
kidcheck/
├─ backend/    API REST (Spring Boot 3.5, Java 17, MySQL 8, Flyway)
├─ web/        Panel (SvelteKit 2 + Svelte 5, SPA con BFF de cookies httpOnly)
├─ android/    App de portería (Kotlin + Compose, offline-first)
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

## App Android de portería

Requisitos: Android Studio (o JDK 17+ + Android SDK 37), un emulador o una tablet
Android 8.0+ (API 26) con cámara.

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- En el emulador usa el servidor `http://10.0.2.2:8080` (es la IP del host).
- En una tablet usa `http://IP-DEL-PC:8080`, con la tablet y la PC en la misma red.
- Usuario demo: `vigilante1` · contraseña `KidCheck2026!`.
- El PIN para salir del modo kiosco es `1234`.
- El modo kiosco de la app mantiene la pantalla encendida, oculta las barras del
  sistema y bloquea el botón atrás. Para un bloqueo total del dispositivo se
  necesitaría un MDM o *device owner*; fuera del alcance de la demo.
- La app guarda el roster en Room cifrado con SQLCipher, los tokens en
  `EncryptedSharedPreferences` y los ingresos en una cola local. WorkManager
  reintenta cada 6 horas y al abrir la app; el botón **Sincronizar** fuerza el
  envío.
- Si la cámara no está disponible, **Buscar alumno** permite el registro manual
  con motivo obligatorio.

Para probar el escaneo en el emulador, muestra el QR de un estudiante en el panel
web (Estudiantes → credencial) y apunta la cámara virtual hacia la pantalla. Si no
es posible, usa la búsqueda manual: el flujo offline y la cola son los mismos.

## Usuarios de la semilla

| Usuario | Rol | Contraseña |
|---|---|---|
| `vigilante1` | vigilante | `KidCheck2026!` |
| `admin1` | administrador | `KidCheck2026!` |
| `auxiliar1` | auxiliar | `KidCheck2026!` |
| `psicologo1` | psicólogo | `KidCheck2026!` |

## Flujo de la demo (S2 → S5)

1. Entrar al panel con `admin1`.
2. **Apoderados** → registrar un contacto familiar (teléfono con código de país).
3. **Estudiantes** → *Nuevo estudiante*: datos, sección y apoderado.
4. Al guardar se emite la credencial: se muestra el QR y se descarga en PNG.
5. En la lista se puede **reemitir** (revoca la anterior) o **revocar** la credencial.

### Reportes, auditoría y configuración (S5)

- **Reportes** (`/reportes`): R01 del día con gráfico de composición, R02 mensual
  con gráfico de barras por estudiante, R03 con inasistencias/reincidencias y
  detalle en tabla. El botón **Descargar CSV** (solo administrador) exporta el
  mismo cálculo que se ve en pantalla.
- **Configuración** (`/configuracion`): umbral de reincidencia, canal principal,
  hora límite y tolerancia por sección, y calendario escolar (días no laborables).
- **Auditoría** (`/auditoria`): quién creó/actualizó/emitió/aprobó/exportó, con
  filtros por entidad y fechas, paginación e IP. No guarda tokens ni contraseñas.
- La semilla incluye asistencias sintéticas de septiembre y octubre de 2026 para
  que los gráficos tengan contenido desde el primer arranque.

### Registro de ingresos (S3 + S5)

- La app de portería (o curl) llama `POST /api/v1/ingresos` con `Idempotency-Key`.
- Para probar sin la app, primero pide el roster y registra un ingreso:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"usuario":"vigilante1","password":"KidCheck2026!"}' \
  | jq -r .accessToken)

curl -s localhost:8080/api/v1/sync/roster -H "Authorization: Bearer $TOKEN" | jq

curl -s -X POST localhost:8080/api/v1/ingresos \
  -H "Authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -H 'Idempotency-Key: demo-0001' \
  -d '{"idEstudiante":3,"deviceUuid":"11111111-1111-1111-1111-111111111111","fechaHoraUtc":"2026-10-23T12:41:05Z","canal":"qr"}'
```

- `POST /api/v1/sync/ingresos` acepta lotes desde la cola offline y devuelve
  `CREADO | DUPLICADO | ERROR` por elemento. El registro manual envía además
  `observacion` con el motivo (por ejemplo `cita_medica`), que queda guardado en
  el ingreso.
- `GET /api/v1/panel/dia` alimenta el panel con polling cada 5 s.
- Las notificaciones se guardan en la tabla `notificacion` (outbox) y un worker
  las envía cada 30 s. Con `TWILIO_ENABLED=false` el envío se simula.
- Solo se notifica a apoderados con consentimiento `notificaciones` en estado
  `autorizado`; la semilla ya los incluye. WhatsApp es el canal principal y el
  correo el respaldo cuando no hay teléfono.
- El job RN-01 marca ausentes pasada la hora límite + tolerancia y genera el
  enlace público `/j/<token>` para que el apoderado justifique en 72 h.
- El job RN-03 genera alertas de reincidencia los viernes a las 16:00 (Lima).
- Reportes: `GET /api/v1/reportes/diario?fecha=2026-10-23`,
  `GET /api/v1/reportes/mensual?periodo=2026-10` y
  `GET /api/v1/reportes/asistencia.csv?periodo=2026-10` (CSV solo admin).
- Auditoría: `GET /api/v1/auditoria?entidad=&desde=&hasta=&pagina=&tamano=`.
- Justificación pública de la semilla: <http://localhost:5173/j/just-abc>
  (sin login; expira en 72 h desde la generación de la alerta).

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
- **Offline-first** (Android): roster cacheado en Room + SQLCipher, cola de ingresos con UUID idempotente, WorkManager y sincronización por lotes. El escaneo no depende de la red.
- **Outbox** para notificaciones WhatsApp (S3), con envío simulado si `TWILIO_ENABLED=false`.
- **Reportes** calculados desde `v_asistencia_mensual` y la tabla de asistencias; JSON y CSV comparten las mismas filas. La auditoría es *best-effort* y nunca bloquea la operación principal.

El plan completo está en `.opencode/plan/kidcheck-plan.md`.
