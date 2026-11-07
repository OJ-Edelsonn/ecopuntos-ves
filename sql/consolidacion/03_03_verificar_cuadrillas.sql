USE ecopuntos_ves;
SELECT (SELECT COUNT(*) FROM cuadrilla) AS cuadrillas,
       (SELECT COUNT(*) FROM miembro_cuadrilla) AS integrantes,
       (SELECT COUNT(*) FROM atencion_punto_critico) AS atenciones;
SELECT c.id_cuadrilla,c.nombre,COALESCE(s.nombre,'Sin sector fijo') sector,
       COUNT(m.id_miembro) integrantes
FROM cuadrilla c LEFT JOIN sector s ON c.id_sector=s.id_sector
LEFT JOIN miembro_cuadrilla m ON c.id_cuadrilla=m.id_cuadrilla
GROUP BY c.id_cuadrilla,c.nombre,s.nombre ORDER BY c.nombre;
SELECT a.id_atencion,a.responsable,c.nombre AS cuadrilla,m.nombres AS persona
FROM atencion_punto_critico a
LEFT JOIN cuadrilla c ON c.id_cuadrilla=a.id_cuadrilla
LEFT JOIN miembro_cuadrilla m ON m.id_miembro=a.id_miembro
ORDER BY a.id_atencion DESC LIMIT 15;
-- Resultado esperado tras instalar: 4 cuadrillas, 12 integrantes.
-- Las atenciones antiguas conservan su campo 'responsable' y referencias NULL.
