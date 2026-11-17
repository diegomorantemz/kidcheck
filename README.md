# KidCheck

Control de ingreso escolar con credencial QR: API Spring Boot, panel web y app Android de portería.

> Proyecto académico con datos ficticios. No cargar datos reales de menores.

## Requisitos

- Docker y Docker Compose (opción recomendada)
- Para ejecución local: Java 17, Maven 3.9, Node 20+ y MySQL 8
- Para Android: Android Studio o SDK 37

## Ejecutar con Docker

```bash
cp .env.example .env   # opcional: cambiar puertos o secretos
docker compose up -d --build
```

Servicios:

- Panel: <http://localhost:5173>
- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>

Comandos útiles:

```bash
docker compose ps
docker compose logs -f api
docker compose down
```

Si el puerto 3306 ya está ocupado, configura `DB_PORT=3307` en `.env`.

## Ejecución local

Con MySQL 8 en `localhost:3306` (usuario `root`, contraseña `root`; ajusta `DB_USER` y `DB_PASSWORD` si es distinto):

```bash
# Terminal 1: API
cd backend
mvn spring-boot:run

# Terminal 2: panel
cd web
npm install
npm run dev
```

La API crea la base `kidcheck` si no existe y Flyway carga la semilla al iniciar.

## App Android

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

La URL de la API se configura al compilar:

```bash
# Emulador (valor por defecto)
KIDCHECK_API_URL=http://10.0.2.2:8080 ./gradlew :app:assembleDebug

# Celular o tablet en la misma red
KIDCHECK_API_URL=http://IP-DE-LA-PC:8080 ./gradlew :app:assembleDebug
```

También se puede guardar en `android/local.properties`:

```properties
kidcheck.api.url=http://IP-DE-LA-PC:8080
```

## Usuarios demo

Todos usan la contraseña `KidCheck2026!`.

| Usuario | Rol |
|---|---|
| `admin1` | administrador |
| `auxiliar1` | auxiliar |
| `vigilante1` | vigilante |
| `psicologo1` | psicólogo |

El PIN para salir del modo kiosco es `1234`.

## Pruebas

```bash
cd backend && mvn test
cd web && npm run check
cd web && npm run test:e2e       # requiere docker compose up
cd android && ./gradlew :app:testDebugUnitTest
```

## Estructura

```text
backend/    API REST (Spring Boot, MySQL, Flyway)
web/        Panel (SvelteKit)
android/    App de portería (Kotlin + Compose)
tests/k6/   Pruebas de carga
docs/       Demo, pruebas y despliegue
```

Más detalles en [docs/DEMO.md](docs/DEMO.md), [docs/TESTING.md](docs/TESTING.md) y [docs/DEPLOY.md](docs/DEPLOY.md).
