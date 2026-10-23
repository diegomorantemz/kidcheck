INSERT INTO colegio (nombre, direccion, distrito, zona, conectividad, estado)
VALUES ('I.E. San Jose', 'Av. Sanchez Cerro 120', 'Piura', 'rural', 'intermitente', 'piloto');

INSERT INTO rol (nombre, descripcion) VALUES
 ('vigilante','Escaneo de ingresos en porteria'),
 ('administrador','Gestion total del colegio'),
 ('auxiliar','Atencion de alertas y justificaciones'),
 ('psicologo','Seguimiento de casos');

-- BCrypt (cost 12) de la contraseña 'KidCheck2026!'
SET @phs = '$2y$12$0xrAVsT89TnEQEdufK60Qu1Y2GzJ71Nkr3tKcIxY90cDPoTTD4lmO';

INSERT INTO usuario (id_colegio,id_rol,nombre_completo,email,usuario_login,contrasena_hash) VALUES
 (1,1,'Luis Vera','vigilante@kidcheck.pe','vigilante1',@phs),
 (1,2,'Carmen Rojas','admin@kidcheck.pe','admin1',@phs),
 (1,3,'Jorge Paredes','auxiliar@kidcheck.pe','auxiliar1',@phs),
 (1,4,'Elena Nunez','psicologo@kidcheck.pe','psicologo1',@phs);

INSERT INTO periodo_escolar (id_colegio,nombre,fecha_inicio,fecha_fin,estado)
VALUES (1,'2026','2026-03-02','2026-12-18','activo');

INSERT INTO seccion (id_periodo,grado,letra,hora_limite_ingreso,tolerancia_minutos) VALUES
 (1,'5','A','07:45:00',30),
 (1,'6','B','08:00:00',30);

INSERT INTO dispositivo (id_colegio,device_uuid,tipo,ubicacion,estado)
VALUES (1,'11111111-1111-1111-1111-111111111111','tablet','Porteria principal','activo');

INSERT INTO estudiante (id_colegio,codigo_matricula,nombres,apellidos,fecha_nacimiento) VALUES
 (1,'2026-0017','Ana Lucia','Perez Ruiz','2015-04-12'),
 (1,'2026-0022','Mateo','Silva Cordova','2014-11-03'),
 (1,'2026-0031','Camila','Torres Vega','2015-01-27');

INSERT INTO apoderado (id_colegio,nombres_apellidos,telefono_whatsapp,email) VALUES
 (1,'Maria Ruiz Serna','+51911111111','maria.ruiz@example.com'),
 (1,'Carlos Silva Lama','+51922222222','carlos.silva@example.com');

INSERT INTO estudiante_apoderado (id_estudiante,id_apoderado,parentesco,es_principal) VALUES
 (1,1,'madre',1),(2,2,'padre',1),(3,1,'madre',1);

INSERT INTO consentimiento_datos (id_estudiante,id_apoderado,finalidad,version_texto,estado,fecha_otorgamiento)
VALUES (1,1,'notificaciones','v1','autorizado',NOW()),
       (2,2,'notificaciones','v1','autorizado',NOW()),
       (3,1,'notificaciones','v1','autorizado',NOW());

INSERT INTO matricula (id_estudiante,id_seccion,estado,fecha_matricula) VALUES
 (1,1,'activa','2026-03-02'),(2,1,'activa','2026-03-02'),(3,2,'activa','2026-03-02');

-- token_hash = SHA2(token, 256); en producción el token es aleatorio de 128 bits
INSERT INTO credencial_qr (id_estudiante,token_hash) VALUES
 (1,SHA2('tok-ana-0017',256)),(2,SHA2('tok-mateo-0022',256)),(3,SHA2('tok-camila-0031',256));

INSERT INTO calendario_escolar (id_colegio,fecha,es_laborable,descripcion)
SELECT 1, d, CASE WHEN DAYOFWEEK(d) IN (1,7) THEN 0 ELSE 1 END,
       CASE WHEN DAYOFWEEK(d) IN (1,7) THEN 'Fin de semana' ELSE 'Clase' END
FROM (SELECT DATE('2026-10-19') + INTERVAL n DAY AS d
      FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) x) dias;

INSERT INTO configuracion (id_colegio,clave,valor) VALUES
 (1,'umbral_reincidencia','3'),
 (1,'zona_horaria','America/Lima'),
 (1,'canal_principal','whatsapp');

-- Escenario de prueba: un ingreso a tiempo, un ausente con alerta y una justificación pendiente
INSERT INTO ingreso (id_estudiante,id_matricula,id_usuario,id_dispositivo,
                     fecha_hora_utc,fecha_local,hora_local,canal,idempotency_key)
VALUES (1,1,1,1,'2026-10-23 12:41:05','2026-10-23','07:41:05','qr',
        '6f1c2a3e-0000-0000-0000-000000000001');

INSERT INTO asistencia_dia (id_estudiante,id_seccion,fecha,estado,minutos_tarde,id_ingreso,origen)
VALUES (1,1,'2026-10-23','presente',0,1,'escaner');

INSERT INTO asistencia_dia (id_estudiante,id_seccion,fecha,estado,origen)
VALUES (2,1,'2026-10-23','ausente','job');

INSERT INTO alerta (id_asistencia,id_estudiante,tipo,estado)
VALUES (2,2,'inasistencia','generada');

INSERT INTO notificacion (id_estudiante,id_apoderado,id_ingreso,canal,plantilla,destino,estado_entrega)
VALUES (1,1,1,'whatsapp','ingreso_confirmado','+51911111111','enviado');

INSERT INTO notificacion (id_estudiante,id_apoderado,id_alerta,canal,plantilla,destino)
VALUES (2,2,1,'whatsapp','alerta_inasistencia','+51922222222');

INSERT INTO justificacion (id_alerta,id_apoderado,motivo,detalle,estado)
VALUES (1,2,'enfermedad','Fiebre, reposo 48 h','pendiente');

INSERT INTO token_justificacion (id_alerta,token_hash,expira_en)
VALUES (1,SHA2('just-abc',256), DATE_ADD(NOW(), INTERVAL 72 HOUR));
