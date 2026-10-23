# KidCheck

Sistema de control de ingreso escolar por credencial QR, con panel web y app Android de portería.

Monorepo:

```
kidcheck/
├─ backend/    API REST (Spring Boot 3.5, Java 17, MySQL 8, Flyway)
├─ web/        Panel de administración (SvelteKit) — pendiente S2
├─ android/    App de portería (Kotlin + Compose) — pendiente S5
└─ docker-compose.yml
```

## Requisitos

- Java 17+
- Maven 3.9+
- MySQL 8.0.16+ (o Docker)
- Node 20+ (solo para `web/`)

## Arranque rápido (backend)

```bash
# 1. Base de datos
docker compose up -d db

# 2. API (Flyway crea el esquema y carga la semilla)
cd backend
mvn spring-boot:run

# 3. Verificar
curl -s http://localhost:8080/actuator/health
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usuario":"admin1","password":"KidCheck2026!","deviceUuid":"11111111-1111-1111-1111-111111111111"}'
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

## Usuarios de la semilla (solo desarrollo)

| Usuario | Rol | Contraseña |
|---|---|---|
| `vigilante1` | vigilante | `KidCheck2026!` |
| `admin1` | administrador | `KidCheck2026!` |
| `auxiliar1` | auxiliar | `KidCheck2026!` |
| `psicologo1` | psicólogo | `KidCheck2026!` |

> Los datos de la semilla son ficticios. Nunca cargar datos reales de menores.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de MySQL |
| `DB_PORT` | `3306` | Puerto |
| `DB_NAME` | `kidcheck` | Base de datos |
| `DB_USER` | `root` | Usuario |
| `DB_PASSWORD` | `root` | Contraseña |
| `JWT_SECRET` | (dev) | Secreto HMAC, mínimo 32 bytes |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Orígenes permitidos de la SPA |

El plan completo del proyecto está en `.opencode/plan/kidcheck-plan.md`.
