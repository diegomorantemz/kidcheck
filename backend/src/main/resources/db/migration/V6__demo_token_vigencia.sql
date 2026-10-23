-- El enlace publico de la semilla se usa en la demo y en los E2E, por lo que
-- se mantiene vigente aunque la base se haya creado hace dias.
-- Regla de negocio intacta: los enlaces que genera RN-01 nacen con 72 h.
UPDATE token_justificacion
   SET expira_en = DATE_ADD(NOW(), INTERVAL 365 DAY)
 WHERE id_alerta = 1
   AND token_hash = SHA2('just-abc', 256);
