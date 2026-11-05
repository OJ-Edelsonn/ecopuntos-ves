-- EcoPuntos VES / EC3 / MariaDB 10.4 (XAMPP)
-- Modelo relacional de 12 tablas segun el documento corregido del proyecto.
-- No elimina tablas ni datos existentes. Ejecutar este archivo ANTES de 02_datos_demostracion.sql.
-- Estas tablas y reglas pertenecen exclusivamente a una demostracion academica.

CREATE DATABASE IF NOT EXISTS ecopuntos_ves
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecopuntos_ves;
SET NAMES utf8mb4;
SET time_zone = '-05:00';

CREATE TABLE IF NOT EXISTS rol (
  id_rol INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(40) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  nombres VARCHAR(80) NOT NULL,
  apellidos VARCHAR(80) NOT NULL,
  correo VARCHAR(100) NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  id_rol INT NOT NULL,
  cambio_clave_pendiente BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sector (
  id_sector INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vecino (
  id_vecino INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL UNIQUE,
  dni VARCHAR(8) NOT NULL UNIQUE,
  direccion VARCHAR(150) NOT NULL,
  telefono VARCHAR(15) NULL,
  codigo_contribuyente VARCHAR(20) NULL,
  id_sector INT NOT NULL,
  CONSTRAINT ck_vecino_dni CHECK (dni REGEXP '^[0-9]{8}$'),
  CONSTRAINT fk_vecino_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_vecino_sector FOREIGN KEY (id_sector) REFERENCES sector(id_sector),
  INDEX ix_vecino_sector (id_sector)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tipo_material (
  id_tipo_material INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS regla_puntos (
  id_regla INT AUTO_INCREMENT PRIMARY KEY,
  concepto VARCHAR(20) NOT NULL,
  id_tipo_material INT NULL,
  valor DECIMAL(10,2) NOT NULL,
  vigente BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_inicio DATETIME NOT NULL,
  fecha_fin DATETIME NULL,
  clave_vigente VARCHAR(40) AS (
    CASE WHEN vigente = 1 THEN CONCAT(concepto, ':', COALESCE(id_tipo_material, 0)) ELSE NULL END
  ) PERSISTENT,
  CONSTRAINT fk_regla_material FOREIGN KEY (id_tipo_material) REFERENCES tipo_material(id_tipo_material),
  CONSTRAINT ck_regla_concepto CHECK (concepto IN ('RECICLAJE','REPORTE','CANJE')),
  CONSTRAINT ck_regla_valor CHECK (valor > 0),
  CONSTRAINT ck_regla_material CHECK (
    (concepto = 'RECICLAJE' AND id_tipo_material IS NOT NULL) OR
    (concepto IN ('REPORTE','CANJE') AND id_tipo_material IS NULL)
  ),
  CONSTRAINT ck_regla_enteros CHECK (concepto = 'RECICLAJE' OR valor = FLOOR(valor)),
  CONSTRAINT ck_regla_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
  UNIQUE KEY uq_regla_vigente (clave_vigente)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS entrega (
  id_entrega INT AUTO_INCREMENT PRIMARY KEY,
  id_vecino INT NOT NULL,
  id_operador INT NOT NULL,
  fecha_hora DATETIME NOT NULL,
  kilos_total DECIMAL(10,2) NOT NULL,
  puntos_otorgados INT NOT NULL,
  id_sector INT NOT NULL,
  CONSTRAINT fk_entrega_vecino FOREIGN KEY (id_vecino) REFERENCES vecino(id_vecino),
  CONSTRAINT fk_entrega_operador FOREIGN KEY (id_operador) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_entrega_sector FOREIGN KEY (id_sector) REFERENCES sector(id_sector),
  CONSTRAINT ck_entrega_cantidades CHECK (kilos_total > 0 AND puntos_otorgados >= 0),
  INDEX ix_entrega_fecha (fecha_hora),
  INDEX ix_entrega_vecino_fecha (id_vecino, fecha_hora),
  INDEX ix_entrega_sector (id_sector)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS detalle_entrega (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_entrega INT NOT NULL,
  id_tipo_material INT NOT NULL,
  kilos DECIMAL(10,2) NOT NULL,
  puntos INT NOT NULL,
  id_regla INT NOT NULL,
  valor_aplicado DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_detalle_entrega FOREIGN KEY (id_entrega) REFERENCES entrega(id_entrega),
  CONSTRAINT fk_detalle_material FOREIGN KEY (id_tipo_material) REFERENCES tipo_material(id_tipo_material),
  CONSTRAINT fk_detalle_regla FOREIGN KEY (id_regla) REFERENCES regla_puntos(id_regla),
  CONSTRAINT ck_detalle_cantidades CHECK (kilos > 0 AND puntos >= 0 AND valor_aplicado > 0),
  UNIQUE KEY uq_detalle_entrega_material (id_entrega, id_tipo_material),
  INDEX ix_detalle_material (id_tipo_material)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reporte_punto_critico (
  id_reporte INT AUTO_INCREMENT PRIMARY KEY,
  id_vecino INT NOT NULL,
  id_sector INT NOT NULL,
  direccion_referencia VARCHAR(200) NOT NULL,
  descripcion VARCHAR(500) NOT NULL,
  tipo_residuo VARCHAR(60) NOT NULL,
  foto_url VARCHAR(255) NULL,
  fecha_hora DATETIME NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'REPORTADO',
  motivo_rechazo VARCHAR(300) NULL,
  id_validador INT NULL,
  fecha_validacion DATETIME NULL,
  ubicacion_normalizada VARCHAR(200) NOT NULL,
  fecha_en_atencion DATETIME NULL,
  clave_ubicacion_abierta VARCHAR(220) AS (
    CASE WHEN estado IN ('REPORTADO','VALIDADO','EN_ATENCION')
      THEN CONCAT(id_sector, ':', ubicacion_normalizada)
      ELSE NULL END
  ) PERSISTENT,
  CONSTRAINT fk_reporte_vecino FOREIGN KEY (id_vecino) REFERENCES vecino(id_vecino),
  CONSTRAINT fk_reporte_sector FOREIGN KEY (id_sector) REFERENCES sector(id_sector),
  CONSTRAINT fk_reporte_validador FOREIGN KEY (id_validador) REFERENCES usuario(id_usuario),
  CONSTRAINT ck_reporte_estado CHECK (estado IN ('REPORTADO','VALIDADO','RECHAZADO','EN_ATENCION','RESUELTO')),
  UNIQUE KEY uq_reporte_ubicacion_abierta (clave_ubicacion_abierta),
  INDEX ix_reporte_sector_estado_fecha (id_sector, estado, fecha_hora),
  INDEX ix_reporte_vecino (id_vecino)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS atencion_punto_critico (
  id_atencion INT AUTO_INCREMENT PRIMARY KEY,
  id_reporte INT NOT NULL UNIQUE,
  responsable VARCHAR(100) NOT NULL,
  fecha_programada DATE NOT NULL,
  fecha_resolucion DATETIME NULL,
  observacion VARCHAR(300) NULL,
  id_funcionario INT NOT NULL,
  CONSTRAINT fk_atencion_reporte FOREIGN KEY (id_reporte) REFERENCES reporte_punto_critico(id_reporte),
  CONSTRAINT fk_atencion_funcionario FOREIGN KEY (id_funcionario) REFERENCES usuario(id_usuario),
  CONSTRAINT ck_atencion_resolucion CHECK (
    fecha_resolucion IS NULL OR (observacion IS NOT NULL AND CHAR_LENGTH(observacion) > 0)
  )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS canje (
  id_canje INT AUTO_INCREMENT PRIMARY KEY,
  id_vecino INT NOT NULL,
  puntos_canjeados INT NOT NULL,
  monto_descuento DECIMAL(10,2) NOT NULL,
  fecha_hora DATETIME NOT NULL,
  codigo_constancia VARCHAR(20) NOT NULL UNIQUE,
  estado VARCHAR(20) NOT NULL DEFAULT 'EMITIDO',
  id_regla INT NOT NULL,
  valor_aplicado INT NOT NULL,
  codigo_contribuyente VARCHAR(20) NOT NULL,
  id_anulador INT NULL,
  fecha_anulacion DATETIME NULL,
  motivo_anulacion VARCHAR(300) NULL,
  CONSTRAINT fk_canje_vecino FOREIGN KEY (id_vecino) REFERENCES vecino(id_vecino),
  CONSTRAINT fk_canje_regla FOREIGN KEY (id_regla) REFERENCES regla_puntos(id_regla),
  CONSTRAINT fk_canje_anulador FOREIGN KEY (id_anulador) REFERENCES usuario(id_usuario),
  CONSTRAINT ck_canje_valores CHECK (puntos_canjeados > 0 AND monto_descuento > 0 AND valor_aplicado > 0),
  CONSTRAINT ck_canje_estado CHECK (
    (estado = 'EMITIDO' AND id_anulador IS NULL AND fecha_anulacion IS NULL AND motivo_anulacion IS NULL) OR
    (estado = 'ANULADO' AND id_anulador IS NOT NULL AND fecha_anulacion IS NOT NULL AND motivo_anulacion IS NOT NULL)
  ),
  INDEX ix_canje_vecino_fecha (id_vecino, fecha_hora)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimiento_puntos (
  id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
  id_vecino INT NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  puntos INT NOT NULL,
  fecha_hora DATETIME NOT NULL,
  id_entrega INT NULL,
  id_reporte INT NULL,
  id_canje INT NULL,
  descripcion VARCHAR(200) NULL,
  id_usuario INT NOT NULL,
  id_regla INT NULL,
  CONSTRAINT fk_mov_vecino FOREIGN KEY (id_vecino) REFERENCES vecino(id_vecino),
  CONSTRAINT fk_mov_entrega FOREIGN KEY (id_entrega) REFERENCES entrega(id_entrega),
  CONSTRAINT fk_mov_reporte FOREIGN KEY (id_reporte) REFERENCES reporte_punto_critico(id_reporte),
  CONSTRAINT fk_mov_canje FOREIGN KEY (id_canje) REFERENCES canje(id_canje),
  CONSTRAINT fk_mov_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_mov_regla FOREIGN KEY (id_regla) REFERENCES regla_puntos(id_regla),
  CONSTRAINT ck_mov_origen CHECK (
    (id_entrega IS NOT NULL) + (id_reporte IS NOT NULL) + (id_canje IS NOT NULL) = 1
  ),
  CONSTRAINT ck_mov_tipo CHECK (
    (tipo = 'ENTREGA' AND id_entrega IS NOT NULL AND puntos >= 0) OR
    (tipo = 'REPORTE' AND id_reporte IS NOT NULL AND puntos > 0 AND id_regla IS NOT NULL) OR
    (tipo = 'CANJE' AND id_canje IS NOT NULL AND puntos < 0) OR
    (tipo = 'REVERSO' AND id_canje IS NOT NULL AND puntos > 0)
  ),
  UNIQUE KEY uq_mov_entrega (id_entrega),
  UNIQUE KEY uq_mov_reporte (id_reporte),
  UNIQUE KEY uq_mov_canje_tipo (id_canje, tipo),
  INDEX ix_mov_vecino_fecha (id_vecino, fecha_hora),
  INDEX ix_mov_tipo_fecha (tipo, fecha_hora)
) ENGINE=InnoDB;

-- Verificacion rapida despues de cargar los datos demo:
-- SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema = 'ecopuntos_ves';
