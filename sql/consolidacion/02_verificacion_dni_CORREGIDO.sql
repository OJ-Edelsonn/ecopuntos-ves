-- ECOPUNTOS VES / CONSOLIDACION ETAPA 02
-- VERIFICACION: solo consultas, sin modificar registros.
USE ecopuntos_ves;

-- RESULTADO ESPERADO: 350 DNI actualizados, 0 DNI secuenciales originales.
SELECT
    SUM(id_vecino BETWEEN 1 AND 350 AND
        BINARY dni = BINARY CAST(10000000 + MOD(53031827 * id_vecino + 27831291, 90000000) AS CHAR)) AS dni_nuevos_correctos,
    SUM(id_vecino BETWEEN 1 AND 350 AND dni = LPAD(id_vecino,8,'0')) AS dni_originales_restantes,
    COUNT(*) AS vecinos_totales
FROM vecino;

-- RESULTADO ESPERADO: 0 DNI repetidos y 0 DNI con formato incorrecto.
SELECT COUNT(*) AS grupos_de_dni_repetidos FROM (
    SELECT dni FROM vecino GROUP BY dni HAVING COUNT(*) > 1
) x;
SELECT COUNT(*) AS dni_formato_incorrecto
FROM vecino WHERE dni NOT REGEXP '^[1-9][0-9]{7}$';

-- Estos registros no modifican login, sector ni claves de usuario.
SELECT v.id_vecino, u.username, v.dni, s.nombre AS sector
FROM vecino v
JOIN usuario u ON u.id_usuario = v.id_usuario
JOIN sector s ON s.id_sector = v.id_sector
WHERE v.id_vecino IN (1,2,3,200,350,351)
ORDER BY v.id_vecino;

-- Los recuentos deben coincidir con 00_auditoria.sql.
SELECT (SELECT COUNT(*) FROM vecino) AS vecinos,
       (SELECT COUNT(*) FROM entrega) AS entregas,
       (SELECT COUNT(*) FROM detalle_entrega) AS detalles,
       (SELECT COUNT(*) FROM reporte_punto_critico) AS reportes,
       (SELECT COUNT(*) FROM canje) AS canjes,
       (SELECT COUNT(*) FROM movimiento_puntos) AS movimientos;
