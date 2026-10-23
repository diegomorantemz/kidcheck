-- Datos sintéticos para que los reportes mensuales y la auditoría tengan
-- contenido desde el primer arranque. No representan personas reales.
-- Se generan solo días de semana entre el 2026-09-01 y el 2026-10-22.

INSERT INTO asistencia_dia (id_estudiante, id_seccion, fecha, estado, minutos_tarde, origen)
WITH RECURSIVE fechas AS (
    SELECT DATE('2026-09-01') AS fecha
    UNION ALL
    SELECT DATE_ADD(fecha, INTERVAL 1 DAY) FROM fechas WHERE fecha < '2026-10-22'
),
dias AS (
    SELECT fecha FROM fechas WHERE DAYOFWEEK(fecha) NOT IN (1, 7)
),
base AS (
    SELECT s.id_estudiante,
           s.id_colegio,
           d.fecha,
           CASE
               WHEN s.id_estudiante = 2 AND MOD(DAYOFMONTH(d.fecha), 5) = 0 THEN 'ausente'
               WHEN s.id_estudiante = 3 AND MOD(DAYOFMONTH(d.fecha), 9) = 0 THEN 'ausente'
               WHEN s.id_estudiante = 1 AND MOD(DAYOFMONTH(d.fecha), 11) = 0 THEN 'tarde'
               WHEN s.id_estudiante = 2 AND MOD(DAYOFMONTH(d.fecha), 7) = 0 THEN 'tarde'
               WHEN s.id_estudiante = 3 AND MOD(DAYOFMONTH(d.fecha), 6) = 0 THEN 'tarde'
               ELSE 'presente'
           END AS estado
      FROM dias d
      CROSS JOIN estudiante s
)
SELECT b.id_estudiante,
       CASE b.id_estudiante WHEN 1 THEN 1 WHEN 2 THEN 1 ELSE 2 END,
       b.fecha,
       b.estado,
       CASE WHEN b.estado = 'tarde' THEN 12 ELSE 0 END,
       'job'
  FROM base b
  LEFT JOIN asistencia_dia a
         ON a.id_estudiante = b.id_estudiante AND a.fecha = b.fecha
 WHERE b.id_colegio = 1
   AND a.id_asistencia IS NULL;

-- Trazabilidad de ejemplo: quién creó, emitió, justificó y exportó.
INSERT INTO auditoria (id_usuario, entidad, id_entidad, accion, datos, ip, creado_en) VALUES
 (2, 'estudiante', 1, 'crear',
  JSON_OBJECT('codigoMatricula', '2026-0017', 'seccionId', 1), '127.0.0.1', '2026-10-16 14:05:00'),
 (2, 'credencial', 1, 'emitir',
  JSON_OBJECT('idEstudiante', 1, 'estado', 'activa'), '127.0.0.1', '2026-10-16 14:06:00'),
 (3, 'asistencia', 1, 'justificar_manual',
  JSON_OBJECT('idEstudiante', 1, 'motivo', 'cita_medica', 'estado', 'justificado'),
  '127.0.0.1', '2026-10-21 09:12:00'),
 (2, 'configuracion', 1, 'actualizar',
  JSON_OBJECT('clave', 'umbral_reincidencia', 'valor', '3'), '127.0.0.1', '2026-10-21 16:40:00'),
 (2, 'reporte', NULL, 'consultar_mensual',
  JSON_OBJECT('periodo', '2026-10', 'seccion', 'todas'), '127.0.0.1', '2026-10-22 18:15:00');
