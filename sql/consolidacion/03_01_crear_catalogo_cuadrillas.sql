-- EcoPuntos VES | Consolidacion 03: catalogo municipal
-- MariaDB (XAMPP). Ejecutar UNA VEZ, tras hacer respaldo de ecopuntos_ves.
-- No borra/modifica datos historicos de atencion_punto_critico.
USE ecopuntos_ves;

CREATE TABLE IF NOT EXISTS cuadrilla (
    id_cuadrilla INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    id_sector INT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_cuadrilla_nombre (nombre),
    CONSTRAINT fk_cuadrilla_sector FOREIGN KEY (id_sector)
      REFERENCES sector (id_sector) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS miembro_cuadrilla (
    id_miembro INT AUTO_INCREMENT PRIMARY KEY,
    id_cuadrilla INT NOT NULL,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    cargo VARCHAR(70) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_miembro_en_cuadrilla (id_cuadrilla, nombres, apellidos),
    UNIQUE KEY uq_fk_miembro_cuadrilla (id_cuadrilla, id_miembro),
    CONSTRAINT fk_miembro_cuadrilla FOREIGN KEY (id_cuadrilla)
      REFERENCES cuadrilla (id_cuadrilla) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Columnas opcionales: las atenciones historicas siguen siendo validas.
ALTER TABLE atencion_punto_critico
  ADD COLUMN IF NOT EXISTS id_cuadrilla INT NULL,
  ADD COLUMN IF NOT EXISTS id_miembro INT NULL;

-- Solo agregar las claves foraneas si aun no existen: reejecucion segura.
DELIMITER $$
DROP PROCEDURE IF EXISTS sp_ecopuntos_fk_cuadrillas$$
CREATE PROCEDURE sp_ecopuntos_fk_cuadrillas()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'atencion_punto_critico'
          AND CONSTRAINT_NAME = 'fk_atencion_cuadrilla'
    ) THEN
        ALTER TABLE atencion_punto_critico
          ADD CONSTRAINT fk_atencion_cuadrilla FOREIGN KEY (id_cuadrilla)
          REFERENCES cuadrilla (id_cuadrilla) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'atencion_punto_critico'
          AND CONSTRAINT_NAME = 'fk_atencion_miembro_grupo'
    ) THEN
        ALTER TABLE atencion_punto_critico
          ADD CONSTRAINT fk_atencion_miembro_grupo FOREIGN KEY (id_cuadrilla, id_miembro)
          REFERENCES miembro_cuadrilla (id_cuadrilla, id_miembro) ON DELETE RESTRICT;
    END IF;
END$$
DELIMITER ;
CALL sp_ecopuntos_fk_cuadrillas();
DROP PROCEDURE sp_ecopuntos_fk_cuadrillas;

SELECT COUNT(*) AS atenciones_historicas FROM atencion_punto_critico;
