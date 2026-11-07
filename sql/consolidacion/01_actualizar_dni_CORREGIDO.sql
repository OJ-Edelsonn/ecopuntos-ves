-- ECOPUNTOS VES / CONSOLIDACION ETAPA 02
-- REEMPLAZAR SOLO LOS 350 DNI ORIGINALES, NO EL VECINO AGREGADO MANUALMENTE.
-- Requiere copia de seguridad reciente de la base ecopuntos_ves.
-- Ejecutar este archivo COMPLETO UNA SOLA VEZ.
USE ecopuntos_ves;

DROP PROCEDURE IF EXISTS sp_ecopuntos_actualizar_dni_demo;
DELIMITER $$
CREATE PROCEDURE sp_ecopuntos_actualizar_dni_demo()
BEGIN
    DECLARE v_candidatos INT DEFAULT 0;
    DECLARE v_choques INT DEFAULT 0;
    DECLARE v_actualizados INT DEFAULT 0;
    DECLARE v_safe_updates INT DEFAULT 1;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET SESSION sql_safe_updates = v_safe_updates;
        DROP TEMPORARY TABLE IF EXISTS tmp_ecopuntos_dni;
        RESIGNAL;
    END;

    SET v_safe_updates = @@SESSION.sql_safe_updates;
    DROP TEMPORARY TABLE IF EXISTS tmp_ecopuntos_dni;
    CREATE TEMPORARY TABLE tmp_ecopuntos_dni (
        id_vecino INT NOT NULL PRIMARY KEY,
        dni_anterior VARCHAR(8) NOT NULL,
        dni_nuevo VARCHAR(8) NOT NULL UNIQUE
    ) ENGINE=InnoDB;

    INSERT INTO tmp_ecopuntos_dni (id_vecino, dni_anterior, dni_nuevo)
    SELECT id_vecino, dni,
           CAST(10000000 + MOD(53031827 * id_vecino + 27831291, 90000000) AS CHAR)
    FROM vecino
    WHERE id_vecino BETWEEN 1 AND 350
      AND dni = LPAD(id_vecino, 8, '0');

    SELECT COUNT(*) INTO v_candidatos FROM tmp_ecopuntos_dni;
    IF v_candidatos <> 350 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abortado: no hay exactamente 350 DNI originales. Revise 00_auditoria.';
    END IF;

    SELECT COUNT(*) INTO v_choques
    FROM vecino v
    JOIN tmp_ecopuntos_dni t ON BINARY v.dni = BINARY t.dni_nuevo AND v.id_vecino <> t.id_vecino;
    IF v_choques > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abortado: DNI propuesto coincide con un DNI existente. No se modifico nada.';
    END IF;

    START TRANSACTION;
    SET SESSION sql_safe_updates = 0;
    UPDATE vecino v
    JOIN tmp_ecopuntos_dni t ON t.id_vecino = v.id_vecino AND t.dni_anterior = v.dni
    SET v.dni = t.dni_nuevo;
    SET v_actualizados = ROW_COUNT();
    SET SESSION sql_safe_updates = v_safe_updates;
    IF v_actualizados <> v_candidatos THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abortado: cantidad actualizada inconsistente.';
    END IF;
    COMMIT;
    SELECT v_actualizados AS dni_actualizados, 'Correcto. Los demas datos no se modificaron.' AS resultado;
    DROP TEMPORARY TABLE IF EXISTS tmp_ecopuntos_dni;
END$$
DELIMITER ;

CALL sp_ecopuntos_actualizar_dni_demo();
DROP PROCEDURE IF EXISTS sp_ecopuntos_actualizar_dni_demo;
