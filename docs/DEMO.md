# Guion de demo — KidCheck (10 minutos)

> Proyecto académico con datos ficticios. No usar datos reales de menores.

## Preparación (antes de entrar al aula)

```bash
cd ~/kidcheck
docker compose up -d --build
docker compose ps
curl -s http://localhost:8080/actuator/health
```

Abrir <http://localhost:5173> y comprobar el login. Tener el APK instalado y la
tablet/emulador con la URL del servidor correcta. Si el puerto 3306 está ocupado
por otro MySQL, usar `DB_PORT=3307` en `.env`.

Usuarios: `admin1`, `auxiliar1`, `vigilante1`, `psicologo1` · contraseña
`KidCheck2026!`. PIN de salida del kiosco: `1234`.

## 1. Problema y alcance (30 s)

- El colegio usa listas de papel; las familias se enteran tarde de una ausencia.
- KidCheck registra el ingreso con una **credencial QR**, sin biometría, y avisa
  por WhatsApp.
- Solo se usan datos sintéticos; la app funciona sin internet en portería.

## 2. Alta y credencial QR (1 min)

1. Entrar como `admin1`.
2. Ir a **Estudiantes**.
3. Mostrar `+ Nuevo estudiante` y explicar matrícula/apoderado.
4. En la lista, abrir **Generar QR** o **Reemitir QR** de Ana Lucía Pérez Ruiz.
5. Mostrar el QR y descargarlo como PNG. Aclarar que en la base solo queda el
   hash SHA-256, nunca la imagen ni el token en claro.

## 3. Escena de la semilla (1 min)

Ir a **Panel del día** y mostrar:

- Ana Lucía figura presente (07:41).
- Mateo Silva figura ausente.
- Hay una alerta/justificación pendiente.

Abrir en otra pestaña el enlace público de la semilla:

<http://localhost:5173/j/just-abc>

Mostrar que no requiere login y que la justificación ya fue registrada. En una
demo con jobs activos, el enlace se genera al marcar la inasistencia y expira en
72 h; el enlace de la semilla se mantiene vigente para no romper la demo.

## 4. Portería offline (3 min)

1. Abrir la app Android e iniciar sesión como `vigilante1`.
2. Mostrar la pantalla de kiosco: cámara, reloj, último sync y botones grandes.
3. Activar **modo avión**.
4. Escanear el QR de Ana o usar **Buscar alumno** y registrar un ingreso manual
   con motivo obligatorio.
5. Repetir el mismo alumno: mostrar el aviso de **duplicado local**.
6. Explicar la cola Room + SQLCipher y el `clientId` idempotente.
7. Desactivar modo avión y pulsar **Sincronizar**.
8. Volver al panel: el ingreso aparece en menos de 10 s por polling.
9. Si se muestra el kiosco, salir con PIN `1234`; aclarar que es un kiosco de
   aplicación, no un bloqueo MDM del dispositivo.

Si no hay cámara disponible, usar **Buscar alumno**. El flujo offline, la cola,
el duplicado y la sincronización son exactamente los mismos.

## 5. Reportes, auditoría y configuración (2 min)

1. Ir a **Reportes**:
   - R01 del día con gráfico de composición.
   - R02 mensual con barras por estudiante.
   - R03 con inasistencias/reincidencias.
2. Pulsar **Descargar CSV** (solo administrador); abrir el archivo y mostrar las
   mismas filas que la tabla.
3. Ir a **Auditoría** y filtrar por `reporte` o `seccion`; mostrar usuario, IP y
   fecha, sin tokens.
4. Ir a **Configuración**:
   - umbral de reincidencia;
   - canal principal;
   - hora límite/tolerancia por sección;
   - calendario escolar y días no laborables.

## 6. Cierre (1 min)

- Arquitectura: Spring Boot + MySQL + SvelteKit (BFF) + Android Kotlin.
- Decisiones: QR de 128 bits con hash, sin biometría; offline-first; outbox de
  notificaciones; panel con roles.
- Limitaciones: WhatsApp simulado si no hay Twilio Sandbox, kiosco sin MDM,
  integración con el sistema oficial del colegio fuera de alcance.
- Estado S6: 13 pruebas de backend, 11 E2E de Playwright y carga k6 superada;
  el APK compila, pasa lint y la prueba JVM de hash. Grabar el video final con
  este mismo guion y conservar los resultados de `docs/TESTING.md`.

## Plan B si falla algo

- **Sin Docker:** usar el arranque local del README (MySQL + Maven + Node).
- **Sin cámara:** búsqueda manual desde la app.
- **Sin tablet:** ejecutar los `curl` de `docs/TESTING.md` o el script de k6
  para demostrar registro y carga.
- **Sin WhatsApp real:** mostrar la tabla `notificacion`; el envío simulado deja
  el outbox y el estado de entrega.
- **Sin internet:** el panel y la app funcionan con el backend local; WhatsApp
  queda en modo simulado.
