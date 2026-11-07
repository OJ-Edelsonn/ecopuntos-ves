-- ECOPUNTOS VES / CONSOLIDACION ETAPA 02
-- AUDITORIA: no modifica registros.
-- MariaDB XAMPP / MySQL Workbench.
USE ecopuntos_ves;

-- Debe mostrar: 351 vecinos y 350 DNI pre-cargados por reemplazar.
-- El vecino creado manualmente (ID 351) se conserva intacto.
SELECT COUNT(*) AS vecinos_totales,
       SUM(id_vecino BETWEEN 1 AND 350 AND dni=LPAD(id_vecino,8,'0')) AS dni_sinteticos_originales
FROM vecino;

-- Muestra una muestra de cambios (identificadores ficticios, no validados por RENIEC).
SELECT id_vecino, dni AS dni_actual,
       CAST(10000000 + MOD(53031827 * id_vecino + 27831291, 90000000) AS CHAR) AS dni_propuesto
FROM vecino
WHERE id_vecino BETWEEN 1 AND 350
  AND dni = LPAD(id_vecino, 8, '0')
ORDER BY id_vecino LIMIT 15;

-- Debe dar 0: los nuevos numeros no deben coincidir con otros vecinos.
SELECT COUNT(*) AS colisiones_dni_nuevo
FROM vecino existente
JOIN (
    SELECT id_vecino,
           CAST(10000000 + MOD(53031827 * id_vecino + 27831291, 90000000) AS CHAR) AS nuevo_dni
    FROM vecino
    WHERE id_vecino BETWEEN 1 AND 350
      AND dni = LPAD(id_vecino, 8, '0')
) propuestas ON BINARY existente.dni = BINARY propuestas.nuevo_dni
           AND existente.id_vecino <> propuestas.id_vecino;

-- Guarda o captura las cifras antes del cambio para compararlas con 02_verificacion.sql.
SELECT (SELECT COUNT(*) FROM vecino) AS vecinos,
       (SELECT COUNT(*) FROM entrega) AS entregas,
       (SELECT COUNT(*) FROM detalle_entrega) AS detalles,
       (SELECT COUNT(*) FROM reporte_punto_critico) AS reportes,
       (SELECT COUNT(*) FROM canje) AS canjes,
       (SELECT COUNT(*) FROM movimiento_puntos) AS movimientos;
