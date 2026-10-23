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

## Flujo de la demo (S2)

1. Entrar al panel con `admin1`.
2. **Apoderados** → registrar un contacto familiar (teléfono con código de país).
3. **Estudiantes** → *Nuevo estudiante*: datos, sección y apoderado.
4. Al guardar se emite la credencial: se muestra el QR y se descarga en PNG.
5. En la lista se puede **reemitir** (revoca la anterior) o **revocar** la credencial.

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

### Panel

| Variable | Default | Descripción |
|---|---|---|
| `API_URL` | `http://localhost:8080` | URL interna del backend (solo servidor) |
| `PORT` | `3000` (`vite dev`: 5173) | Puerto del servidor Node |
| `ORIGIN` | — | Origen público cuando se despliega detrás de un proxy |

## Decisiones clave

- **QR sin biometría**: token aleatorio de 128 bits; en la base solo se guarda `SHA-256(token)`. El QR contiene el token en claro y la app compara hashes offline.
- **BFF**: el navegador nunca ve el JWT. Las cookies `kc_access` y `kc_refresh` son `httpOnly` y el proxy de SvelteKit renueva la sesión de forma transparente.
- **Offline-first** (Android, S5): roster cacheado + cola Room + sincronización idempotente por `Idempotency-Key`.
- **Outbox** para notificaciones WhatsApp (S3).

El plan completo está en `.opencode/plan/kidcheck-plan.md`.
