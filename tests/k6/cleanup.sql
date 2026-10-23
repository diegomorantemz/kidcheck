-- Limpieza opcional después de ejecutar tests/k6/kidcheck.js.
-- El escenario de sync usa una fecha futura para no alterar la escena demo.
DELETE FROM notificacion
 WHERE id_ingreso IN (
   SELECT id_ingreso FROM ingreso WHERE fecha_local = '2026-12-31'
 );
DELETE FROM asistencia_dia WHERE fecha = '2026-12-31';
DELETE FROM ingreso WHERE fecha_local = '2026-12-31';
