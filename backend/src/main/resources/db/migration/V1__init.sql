CREATE TABLE colegio (
  id_colegio      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(150) NOT NULL,
  direccion       VARCHAR(200) NULL,
  distrito        VARCHAR(100) NULL,
  zona            VARCHAR(15)  NOT NULL DEFAULT 'rural'
                  CHECK (zona IN ('rural','semiurbana','urbana')),
  conectividad    VARCHAR(15)  NOT NULL DEFAULT 'intermitente'
                  CHECK (conectividad IN ('estable','intermitente','sin_conexion')),
  estado          VARCHAR(10)  NOT NULL DEFAULT 'piloto'
                  CHECK (estado IN ('piloto','activo','inactivo')),
  creado_en       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE rol (
  id_rol      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre      VARCHAR(20) NOT NULL UNIQUE
              CHECK (nombre IN ('vigilante','administrador','auxiliar','psicologo')),
  descripcion VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE usuario (
  id_usuario        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio        BIGINT UNSIGNED NOT NULL,
  id_rol            BIGINT UNSIGNED NOT NULL,
  nombre_completo   VARCHAR(150) NOT NULL,
  email             VARCHAR(150) NOT NULL,
  usuario_login     VARCHAR(50)  NOT NULL,
  contrasena_hash   VARCHAR(100) NOT NULL,
  estado            VARCHAR(12)  NOT NULL DEFAULT 'activo'
                    CHECK (estado IN ('activo','inactivo','bloqueado')),
  intentos_fallidos INT NOT NULL DEFAULT 0,
  ultimo_acceso     DATETIME NULL,
  creado_en         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- MVP de un solo colegio: login unico global para evitar ambiguedad
  UNIQUE KEY uq_usuario_login (usuario_login),
  UNIQUE KEY uq_usuario_email (email),
  CONSTRAINT fk_usuario_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio),
  CONSTRAINT fk_usuario_rol     FOREIGN KEY (id_rol)     REFERENCES rol(id_rol)
) ENGINE=InnoDB;

CREATE TABLE dispositivo (
  id_dispositivo         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio             BIGINT UNSIGNED NOT NULL,
  device_uuid            VARCHAR(36) NOT NULL,
  tipo                   VARCHAR(10) NOT NULL CHECK (tipo IN ('celular','tablet')),
  ubicacion              VARCHAR(100) NULL,
  estado                 VARCHAR(15) NOT NULL DEFAULT 'activo'
                         CHECK (estado IN ('activo','mantenimiento','inactivo')),
  ultima_sincronizacion  DATETIME NULL,
  UNIQUE KEY uq_dispositivo_uuid (device_uuid),
  CONSTRAINT fk_dispositivo_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio)
) ENGINE=InnoDB;

CREATE TABLE periodo_escolar (
  id_periodo   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio   BIGINT UNSIGNED NOT NULL,
  nombre       VARCHAR(20) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin    DATE NOT NULL,
  estado       VARCHAR(12) NOT NULL DEFAULT 'activo'
               CHECK (estado IN ('planificado','activo','cerrado')),
  UNIQUE KEY uq_periodo (id_colegio, nombre),
  CONSTRAINT fk_periodo_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio)
) ENGINE=InnoDB;

CREATE TABLE seccion (
  id_seccion           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_periodo           BIGINT UNSIGNED NOT NULL,
  grado                VARCHAR(10) NOT NULL,
  letra                VARCHAR(5)  NOT NULL,
  hora_limite_ingreso  TIME NOT NULL,
  tolerancia_minutos   SMALLINT UNSIGNED NOT NULL DEFAULT 30,
  UNIQUE KEY uq_seccion (id_periodo, grado, letra),
  CONSTRAINT fk_seccion_periodo FOREIGN KEY (id_periodo) REFERENCES periodo_escolar(id_periodo)
) ENGINE=InnoDB;

CREATE TABLE calendario_escolar (
  id_calendario BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio    BIGINT UNSIGNED NOT NULL,
  fecha         DATE NOT NULL,
  es_laborable  TINYINT NOT NULL DEFAULT 1,
  descripcion   VARCHAR(100) NULL,
  UNIQUE KEY uq_calendario (id_colegio, fecha),
  CONSTRAINT fk_calendario_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio)
) ENGINE=InnoDB;

CREATE TABLE estudiante (
  id_estudiante    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio       BIGINT UNSIGNED NOT NULL,
  codigo_matricula VARCHAR(20) NOT NULL,
  nombres          VARCHAR(100) NOT NULL,
  apellidos        VARCHAR(100) NOT NULL,
  fecha_nacimiento DATE NULL,
  estado           VARCHAR(12) NOT NULL DEFAULT 'activo'
                   CHECK (estado IN ('activo','inactivo','egresado')),
  creado_en        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_estudiante_matricula (id_colegio, codigo_matricula),
  KEY idx_estudiante_apellidos (apellidos, nombres),
  CONSTRAINT fk_estudiante_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio)
) ENGINE=InnoDB;

CREATE TABLE credencial_qr (
  id_credencial BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_estudiante BIGINT UNSIGNED NOT NULL,
  token_hash    VARCHAR(64) NOT NULL,
  estado        VARCHAR(10) NOT NULL DEFAULT 'activa'
                CHECK (estado IN ('activa','revocada')),
  emitida_en    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  revocada_en   DATETIME NULL,
  activa        TINYINT GENERATED ALWAYS AS (IF(estado='activa',1,NULL)) STORED,
  UNIQUE KEY uq_credencial_hash (token_hash),
  UNIQUE KEY uq_credencial_activa (id_estudiante, activa),
  CONSTRAINT fk_credencial_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante)
) ENGINE=InnoDB;

CREATE TABLE matricula (
  id_matricula    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_estudiante   BIGINT UNSIGNED NOT NULL,
  id_seccion      BIGINT UNSIGNED NOT NULL,
  estado          VARCHAR(12) NOT NULL DEFAULT 'activa'
                  CHECK (estado IN ('activa','retirada','trasladada')),
  fecha_matricula DATE NOT NULL,
  UNIQUE KEY uq_matricula (id_estudiante, id_seccion),
  CONSTRAINT fk_matricula_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_matricula_seccion    FOREIGN KEY (id_seccion)    REFERENCES seccion(id_seccion)
) ENGINE=InnoDB;

CREATE TABLE apoderado (
  id_apoderado      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio        BIGINT UNSIGNED NOT NULL,
  nombres_apellidos VARCHAR(150) NOT NULL,
  telefono_whatsapp VARCHAR(20) NULL,
  email             VARCHAR(150) NULL,
  estado            VARCHAR(10) NOT NULL DEFAULT 'activo'
                    CHECK (estado IN ('activo','inactivo')),
  UNIQUE KEY uq_apoderado_telefono (id_colegio, telefono_whatsapp),
  CONSTRAINT fk_apoderado_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio)
) ENGINE=InnoDB;

CREATE TABLE estudiante_apoderado (
  id_estudiante      BIGINT UNSIGNED NOT NULL,
  id_apoderado       BIGINT UNSIGNED NOT NULL,
  parentesco         VARCHAR(15) NOT NULL
                     CHECK (parentesco IN ('padre','madre','apoderado','otro')),
  es_principal       TINYINT NOT NULL DEFAULT 0,
  autorizado_recoger TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id_estudiante, id_apoderado),
  CONSTRAINT fk_ea_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_ea_apoderado  FOREIGN KEY (id_apoderado)  REFERENCES apoderado(id_apoderado)
) ENGINE=InnoDB;

CREATE TABLE consentimiento_datos (
  id_consentimiento   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_estudiante       BIGINT UNSIGNED NOT NULL,
  id_apoderado        BIGINT UNSIGNED NOT NULL,
  finalidad           VARCHAR(30) NOT NULL
                      CHECK (finalidad IN ('notificaciones','datos_personales','imagen')),
  version_texto       VARCHAR(20) NOT NULL,
  estado              VARCHAR(12) NOT NULL DEFAULT 'pendiente'
                      CHECK (estado IN ('autorizado','pendiente','revocado')),
  fecha_otorgamiento  DATETIME NULL,
  fecha_revocacion    DATETIME NULL,
  documento_url       VARCHAR(255) NULL,
  UNIQUE KEY uq_consentimiento (id_estudiante, id_apoderado, finalidad),
  CONSTRAINT fk_consent_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_consent_apoderado  FOREIGN KEY (id_apoderado)  REFERENCES apoderado(id_apoderado)
) ENGINE=InnoDB;

CREATE TABLE ingreso (
  id_ingreso       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_estudiante    BIGINT UNSIGNED NOT NULL,
  id_matricula     BIGINT UNSIGNED NOT NULL,
  id_usuario       BIGINT UNSIGNED NULL,
  id_dispositivo   BIGINT UNSIGNED NULL,
  fecha_hora_utc   DATETIME NOT NULL,
  fecha_local      DATE NOT NULL,
  hora_local       TIME NOT NULL,
  canal            VARCHAR(10) NOT NULL DEFAULT 'qr' CHECK (canal IN ('qr','manual')),
  idempotency_key  VARCHAR(36) NOT NULL,
  observacion      VARCHAR(255) NULL,
  creado_en        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_ingreso_idem (idempotency_key),
  KEY idx_ingreso_fecha (fecha_local, id_estudiante),
  KEY idx_ingreso_est (id_estudiante, fecha_local),
  CONSTRAINT fk_ingreso_estudiante  FOREIGN KEY (id_estudiante)  REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_ingreso_matricula   FOREIGN KEY (id_matricula)   REFERENCES matricula(id_matricula),
  CONSTRAINT fk_ingreso_usuario     FOREIGN KEY (id_usuario)     REFERENCES usuario(id_usuario),
  CONSTRAINT fk_ingreso_dispositivo FOREIGN KEY (id_dispositivo) REFERENCES dispositivo(id_dispositivo)
) ENGINE=InnoDB;

CREATE TABLE asistencia_dia (
  id_asistencia  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_estudiante  BIGINT UNSIGNED NOT NULL,
  id_seccion     BIGINT UNSIGNED NOT NULL,
  fecha          DATE NOT NULL,
  estado         VARCHAR(12) NOT NULL
                 CHECK (estado IN ('presente','tarde','ausente','justificado')),
  minutos_tarde  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  id_ingreso     BIGINT UNSIGNED NULL,
  origen         VARCHAR(10) NOT NULL DEFAULT 'escaner'
                 CHECK (origen IN ('escaner','job','manual')),
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_asistencia_dia (id_estudiante, fecha),
  KEY idx_asistencia_fecha (fecha, id_seccion, estado),
  CONSTRAINT fk_asistencia_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_asistencia_seccion    FOREIGN KEY (id_seccion)    REFERENCES seccion(id_seccion),
  CONSTRAINT fk_asistencia_ingreso    FOREIGN KEY (id_ingreso)    REFERENCES ingreso(id_ingreso)
) ENGINE=InnoDB;

CREATE TABLE alerta (
  id_alerta         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_asistencia     BIGINT UNSIGNED NULL,
  id_estudiante     BIGINT UNSIGNED NOT NULL,
  tipo              VARCHAR(15) NOT NULL
                    CHECK (tipo IN ('inasistencia','reincidencia','tardanza')),
  umbral            SMALLINT UNSIGNED NULL,
  estado            VARCHAR(15) NOT NULL DEFAULT 'generada'
                    CHECK (estado IN ('generada','notificada','justificada','atendida','cerrada')),
  fecha_generacion  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_usuario_atencion BIGINT UNSIGNED NULL,
  fecha_atencion    DATETIME NULL,
  UNIQUE KEY uq_alerta_asistencia (id_asistencia, tipo),
  KEY idx_alerta_pendientes (estado, fecha_generacion),
  CONSTRAINT fk_alerta_asistencia FOREIGN KEY (id_asistencia) REFERENCES asistencia_dia(id_asistencia),
  CONSTRAINT fk_alerta_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_alerta_usuario    FOREIGN KEY (id_usuario_atencion) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE notificacion (
  id_notificacion BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_estudiante   BIGINT UNSIGNED NOT NULL,
  id_apoderado    BIGINT UNSIGNED NULL,
  id_ingreso      BIGINT UNSIGNED NULL,
  id_alerta       BIGINT UNSIGNED NULL,
  canal           VARCHAR(10) NOT NULL CHECK (canal IN ('whatsapp','email','app')),
  plantilla       VARCHAR(40) NOT NULL,
  destino         VARCHAR(150) NOT NULL,
  estado_entrega  VARCHAR(12) NOT NULL DEFAULT 'pendiente'
                  CHECK (estado_entrega IN ('pendiente','enviado','entregado','leido','fallido','cancelado')),
  intentos        INT NOT NULL DEFAULT 0,
  proximo_intento DATETIME NULL,
  proveedor       VARCHAR(20) NULL,
  id_externo      VARCHAR(100) NULL,
  error_codigo    VARCHAR(30) NULL,
  error_mensaje   VARCHAR(255) NULL,
  creado_en       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  enviado_en      DATETIME NULL,
  KEY idx_notif_pendientes (estado_entrega, proximo_intento),
  KEY idx_notif_estudiante (id_estudiante, creado_en),
  CONSTRAINT fk_notif_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
  CONSTRAINT fk_notif_apoderado  FOREIGN KEY (id_apoderado)  REFERENCES apoderado(id_apoderado),
  CONSTRAINT fk_notif_ingreso    FOREIGN KEY (id_ingreso)    REFERENCES ingreso(id_ingreso),
  CONSTRAINT fk_notif_alerta     FOREIGN KEY (id_alerta)     REFERENCES alerta(id_alerta)
) ENGINE=InnoDB;

CREATE TABLE justificacion (
  id_justificacion   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_alerta          BIGINT UNSIGNED NOT NULL,
  id_apoderado       BIGINT UNSIGNED NOT NULL,
  motivo             VARCHAR(20) NOT NULL
                     CHECK (motivo IN ('enfermedad','cita_medica','viaje','emergencia_familiar','otro')),
  detalle            VARCHAR(500) NULL,
  evidencia_url      VARCHAR(255) NULL,
  estado             VARCHAR(10) NOT NULL DEFAULT 'pendiente'
                     CHECK (estado IN ('pendiente','aprobada','rechazada')),
  id_usuario_revision BIGINT UNSIGNED NULL,
  fecha_envio        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_revision     DATETIME NULL,
  UNIQUE KEY uq_justificacion_alerta (id_alerta),
  CONSTRAINT fk_just_alerta    FOREIGN KEY (id_alerta)    REFERENCES alerta(id_alerta),
  CONSTRAINT fk_just_apoderado FOREIGN KEY (id_apoderado) REFERENCES apoderado(id_apoderado),
  CONSTRAINT fk_just_usuario   FOREIGN KEY (id_usuario_revision) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE token_justificacion (
  id_token    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_alerta   BIGINT UNSIGNED NOT NULL,
  token_hash  VARCHAR(64) NOT NULL,
  expira_en   DATETIME NOT NULL,
  usado_en    DATETIME NULL,
  UNIQUE KEY uq_token_just (token_hash),
  CONSTRAINT fk_token_alerta FOREIGN KEY (id_alerta) REFERENCES alerta(id_alerta)
) ENGINE=InnoDB;

CREATE TABLE auditoria (
  id_auditoria BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_usuario   BIGINT UNSIGNED NULL,
  entidad      VARCHAR(40) NOT NULL,
  id_entidad   BIGINT UNSIGNED NULL,
  accion       VARCHAR(30) NOT NULL,
  datos        JSON NULL,
  ip           VARCHAR(45) NULL,
  creado_en    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_auditoria_fecha (creado_en, entidad),
  CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE configuracion (
  id_configuracion BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_colegio       BIGINT UNSIGNED NOT NULL,
  clave            VARCHAR(50) NOT NULL,
  valor            VARCHAR(255) NOT NULL,
  UNIQUE KEY uq_config (id_colegio, clave),
  CONSTRAINT fk_config_colegio FOREIGN KEY (id_colegio) REFERENCES colegio(id_colegio)
) ENGINE=InnoDB;

-- Vista que reemplaza a la tabla reporte_asistencia
CREATE OR REPLACE VIEW v_asistencia_mensual AS
SELECT a.id_estudiante,
       a.id_seccion,
       DATE_FORMAT(a.fecha, '%Y-%m') AS periodo,
       COUNT(*)                                                     AS dias_registrados,
       SUM(a.estado IN ('presente','tarde'))                        AS dias_presente,
       SUM(a.estado = 'tarde')                                      AS dias_tarde,
       SUM(a.estado = 'ausente')                                    AS dias_ausente,
       SUM(a.estado = 'justificado')                                AS dias_justificado,
       ROUND(100 * SUM(a.estado IN ('presente','tarde'))
             / NULLIF(COUNT(*), 0), 2)                              AS porcentaje_asistencia
FROM asistencia_dia a
GROUP BY a.id_estudiante, a.id_seccion, periodo;
