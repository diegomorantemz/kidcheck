-- Un mismo hecho no puede generar dos mensajes de la misma plantilla.
-- Los NULL (por ejemplo, notificaciones de alerta en el índice de ingreso)
-- no chocan entre sí en MySQL.
CREATE UNIQUE INDEX uq_notif_ingreso_plantilla ON notificacion (id_ingreso, plantilla);
CREATE UNIQUE INDEX uq_notif_alerta_plantilla ON notificacion (id_alerta, plantilla);
