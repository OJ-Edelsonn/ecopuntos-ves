-- Datos ficticios de DEMOSTRACION, no son empleados reales del municipio.
USE ecopuntos_ves;
INSERT IGNORE INTO cuadrilla (nombre, id_sector, activo) VALUES
  ('Cuadrilla de limpieza 01', 1, TRUE),
  ('Cuadrilla de limpieza 02', 2, TRUE),
  ('Cuadrilla de limpieza 03', 3, TRUE),
  ('Cuadrilla de limpieza 04', 4, TRUE);

INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Carlos', 'Ramos Quispe', 'Coordinador', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 01';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Lucia', 'Flores Vega', 'Operaria de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 01';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Miguel', 'Salazar Rojas', 'Operario de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 01';

INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Rosa', 'Huaman Torres', 'Coordinadora', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 02';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Jorge', 'Diaz Ramos', 'Operario de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 02';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Elena', 'Paredes Soto', 'Operaria de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 02';

INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Pedro', 'Castillo Vera', 'Coordinador', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 03';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Ana', 'Lopez Ruiz', 'Operaria de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 03';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Julio', 'Mendoza Cruz', 'Operario de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 03';

INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Mariana', 'Vargas Perez', 'Coordinadora', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 04';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'David', 'Quispe Chavez', 'Operario de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 04';
INSERT IGNORE INTO miembro_cuadrilla (id_cuadrilla, nombres, apellidos, cargo, activo)
SELECT id_cuadrilla, 'Sofia', 'Nunez Rojas', 'Operaria de limpieza', TRUE
FROM cuadrilla WHERE nombre = 'Cuadrilla de limpieza 04';
