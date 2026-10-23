-- Cuerpo renderizado de la notificación (outbox).
-- Se guarda al momento de encolar para que el worker sea independiente de la
-- lógica de negocio y para conservar el enlace de justificación (token de un
-- solo uso) sin necesidad de reconstruirlo después.
ALTER TABLE notificacion
  ADD COLUMN cuerpo VARCHAR(1000) NULL AFTER destino;
