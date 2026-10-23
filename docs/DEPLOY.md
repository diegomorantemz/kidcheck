# Despliegue de KidCheck

> KidCheck es un proyecto académico. Si se publica, debe usar datos ficticios o
> contar con autorización y medidas de protección de datos de menores.

## 1. Despliegue local completo (Docker Compose)

Es el entorno usado para la demo y las pruebas E2E:

```bash
cd ~/kidcheck
cp .env.example .env
# editar .env: DB_PORT=3307 si el 3306 ya está ocupado, y un JWT_SECRET real
docker compose up -d --build
docker compose ps
curl -s http://localhost:8080/actuator/health
```

Servicios:

- panel: <http://localhost:5173>
- API: <http://localhost:8080>
- MySQL: `localhost:3307` en este equipo (solo para administración)

## 2. Servidor pequeño (VPS) con HTTPS

Requisitos: Ubuntu 24.04, Docker Engine + Compose, un dominio y puertos 80/443
abiertos. No publiques MySQL ni la API a internet.

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker "$USER"   # cerrar y volver a entrar
git clone https://ejemplo/kidcheck.git kidcheck && cd kidcheck
cp .env.example .env
```

Valores recomendados en `.env` para producción:

```dotenv
API_PORT=127.0.0.1:8080
WEB_PORT=127.0.0.1:5173
DB_PORT=127.0.0.1:3307
DB_PASSWORD=<password-largo-y-aleatorio>
JWT_SECRET=<openssl rand -hex 32>
CORS_ORIGINS=https://panel.example.com
WEB_PUBLIC_URL=https://panel.example.com
WEB_ORIGIN=https://panel.example.com
KIDCHECK_JOBS_ENABLED=true
TWILIO_ENABLED=true
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
```

Al publicar los puertos solo en `127.0.0.1`, Caddy/Nginx puede hacer el proxy
sin exponer la API ni la base de datos. Ejemplo con Caddy:

```caddyfile
panel.example.com {
    encode gzip
    reverse_proxy 127.0.0.1:5173
}

# Solo si la tablet debe llegar a la API por internet.
api.example.com {
    encode gzip
    reverse_proxy 127.0.0.1:8080
}
```

```bash
sudo caddy reload --config /etc/caddy/Caddyfile
```

Comprobar:

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f --tail=100 api
curl -s https://panel.example.com/login -o /dev/null -w '%{http_code}\n'
```

## 3. WhatsApp con Twilio Sandbox

1. Entrar al Twilio Console → Messaging → Try WhatsApp.
2. Copiar el `Account SID`, `Auth Token` y el número del sandbox.
3. Un apoderado debe enviar el código `join <sandbox>` al número del sandbox.
4. Poner `TWILIO_ENABLED=true` y reiniciar la API:

```bash
docker compose up -d --force-recreate api
docker compose logs -f api | grep -i notificacion
```

Si no hay credenciales, dejar `TWILIO_ENABLED=false`: el outbox y los estados se
simulan y la demo sigue funcionando.

## 4. APK para la tablet

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

El APK de demo queda en `android/app/build/outputs/apk/debug/`. Para una
publicación real habría que firmar el release y configurar HTTPS. Si la tablet
usa la red local, apuntar al host (`http://IP-DEL-PC:8080`); si usa el servidor,
apuntar a `https://api.example.com` y mantener la API detrás de Caddy.

## 5. Operación y respaldo

```bash
# Copia de seguridad
docker exec kidcheck-db sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" kidcheck' \
  > respaldo-$(date +%F).sql

# Actualizar
git pull
docker compose up -d --build

# Ver estado
docker compose ps
docker compose logs --tail=200 api web
```

No se incluye una URL pública en el repositorio: el despliegue real requiere
dominio, credenciales de Twilio y una cuenta de servidor. El entorno Docker
local es el que se valida en `docs/TESTING.md`.
