-- Datos de demostración para el desarrollo local. NO los ejecute sobre una base con datos reales:
-- supone `universidad` recién creada con db/init/01-schema.sql (o migrada con 002) y vacía.
--
--   mysql -u root -p universidad < db/seed/datos-demo.sql
--
-- Los nombres de facultades, carreras y cursos son de ejemplo: ajústelos a la malla real antes de
-- mostrar el sistema. Los ids van explícitos para que las claves foráneas cuadren.
-- Este archivo NO inserta cuentas de acceso: al terminar la tabla `usuarios` queda vacía y todavía no se
-- puede entrar. Las cuentas se crean con la aplicación, que es la que calcula el hash PBKDF2 (vea el
-- comentario del final y el paso 5 de docs/instalacion-paso-a-paso.md).

USE universidad;

INSERT INTO `facultades` (`id_facultad`, `nombre`, `decano`) VALUES
  (1, 'Facultad de Ciencias',                'María Fernanda Ávila'),
  (2, 'Facultad de Ingeniería',              'Carlos Julio Prada'),
  (3, 'Facultad de Humanidades y Educación', 'Luz Amparo Quiroga'),
  (4, 'Facultad de Salud',                   'Hernán Darío Ospina'),
  (5, 'Facultad de Administración',          'Silvia Rocío Cardona');

INSERT INTO `carreras` (`id_carrera`, `nombre`, `id_facultad`) VALUES
  (1, 'Agronomía',                        1),
  (2, 'Biología',                         1),
  (3, 'Ingeniería Civil',                 2),
  (4, 'Ingeniería Ambiental',             2),
  (5, 'Licenciatura en Matemáticas',      3),
  (6, 'Medicina Veterinaria y Zootecnia', 4),
  (7, 'Contaduría Pública',               5);

INSERT INTO `profesores` (`id_profesor`, `nombre`, `apellido`, `email`, `id_facultad`) VALUES
  (1, 'Jorge Iván', 'Cardozo',  'jcardozo@ut.edu.co',  1),
  (2, 'Marcela',    'Duarte',   'mduarte@ut.edu.co',   1),
  (3, 'Nelson',     'Quintero', 'nquintero@ut.edu.co', 2),
  (4, 'Adriana',    'Bustos',   'abustos@ut.edu.co',   2),
  (5, 'Fabián',     'Niño',     'fnino@ut.edu.co',     3),
  (6, 'Clara Inés', 'Galvis',   'cgalvis@ut.edu.co',   4),
  (7, 'Ómar',       'Triana',   'otriana@ut.edu.co',   5);

INSERT INTO `cursos` (`id_curso`, `nombre`, `creditos`, `id_carrera`, `id_profesor`) VALUES
  (1,  'Cálculo Diferencial',   4, 3, 3),
  (2,  'Álgebra Lineal',        3, 3, 5),
  (3,  'Estática',              3, 3, 4),
  (4,  'Física de Ondas',       3, 4, 4),
  (5,  'Química General',       4, 1, 2),
  (6,  'Taxonomía Vegetal',     3, 1, 1),
  (7,  'Genética',              3, 2, 2),
  (8,  'Sanidad Animal',        4, 6, 6),
  (9,  'Contabilidad de Costos', 3, 7, 7),
  (10, 'Matemática Discreta',   3, 5, 5);

-- 2026-1 ya cerró: no admite matrículas nuevas (ServicioMatriculas.asegurarPeriodoAbierto).
INSERT INTO `periodos_academicos` (`id_periodo`, `nombre`, `fecha_inicio`, `fecha_fin`, `estado`) VALUES
  (1, '2026-1', '2026-01-19', '2026-06-14', 'CERRADO'),
  (2, '2026-2', '2026-07-15', '2026-11-30', 'ACTIVO'),
  (3, '2027-1', '2027-01-18', '2027-06-12', 'INACTIVO');

INSERT INTO `estudiantes` (`id_estudiante`, `nombre`, `apellido`, `email`, `fecha_nacimiento`, `id_carrera`) VALUES
  (1, 'Ana María',    'Rojas',     'amaroja@ut.edu.co',   '2003-04-11', 3),
  (2, 'David',        'Forero',    'dforero@ut.edu.co',   '2002-11-02', 3),
  (3, 'Sara',         'Mejía',     'smejia@ut.edu.co',    '2004-01-27', 1),
  (4, 'Juan Esteban', 'Parra',     'jparra@ut.edu.co',    '2003-09-08', 1),
  (5, 'Laura',        'Sosa',      'lsosa@ut.edu.co',     '2003-06-30', 2),
  (6, 'Camilo',       'Ordoñez',   'cordonez@ut.edu.co',  '2002-03-15', 6),
  (7, 'Valentina',    'Cifuentes', 'vcifuentes@ut.edu.co','2004-08-19', 7),
  (8, 'Andrés',       'Bello',     'abello@ut.edu.co',    '2003-12-05', 5);

-- Dos grupos de Cálculo en 2026-2 y el aula B-101 repetida entre secciones de cursos distintos: la
-- clave única solo choca cuando se repiten curso, periodo y aula a la vez. La 11 (Física de Ondas,
-- carrera Ingeniería Ambiental) queda sin matriculados: sirve para probar los cupos, y un estudiante
-- de otra carrera no puede inscribirse en ella aunque esté vacía.
INSERT INTO `secciones` (`id_seccion`, `id_curso`, `id_profesor`, `id_periodo`, `aula`, `cupo_maximo`) VALUES
  (1,  1, 3, 2, 'B-101', 30),
  (2,  1, 3, 2, 'B-102', 25),
  (3,  2, 5, 2, 'B-101', 28),
  (4,  3, 4, 2, 'A-204', 20),
  (5,  5, 2, 2, 'Lab-1', 24),
  (6,  6, 1, 2, 'C-3',   18),
  (7,  7, 2, 2, 'Lab-2', 20),
  (8,  8, 6, 2, 'D-1',   15),
  (9,  9, 7, 2, 'E-105', 32),
  (10, 10, 5, 2, 'B-201', 26),
  (11, 4, 4, 2, 'A-101', 22),
  (12, 1, 3, 1, 'B-101', 30),
  (13, 5, 2, 1, 'Lab-1', 24);

-- Matrículas: nadie pasa de 10 créditos en el periodo y todas las secciones son de la carrera
-- del estudiante. `nota` es el promedio de la calificación definitiva (30 % + 30 % + 40 %), dos
-- decimales, y `estado` respeta la regla de asistencia. Las dos últimas son de 2026-1, ya cerrado.
INSERT INTO `matriculas` (`id_matricula`, `id_estudiante`, `id_seccion`, `id_curso`, `periodo`, `nota`, `estado`) VALUES
  (1,  1, 1,  1,  '2026-2', 3.77, 'APROBADO'),
  (2,  1, 3,  2,  '2026-2', NULL, 'EN_CURSO'),
  (3,  2, 1,  1,  '2026-2', 4.07, 'PERDIDO_POR_FALTA_DE_ASISTENCIA'),
  (4,  2, 4,  3,  '2026-2', NULL, 'EN_CURSO'),
  (5,  3, 5,  5,  '2026-2', NULL, 'EN_CURSO'),
  (6,  3, 6,  6,  '2026-2', NULL, 'EN_CURSO'),
  (7,  4, 5,  5,  '2026-2', 3.13, 'APROBADO'),
  (8,  5, 7,  7,  '2026-2', NULL, 'EN_CURSO'),
  (9,  6, 8,  8,  '2026-2', 4.82, 'PERDIDO_POR_FALTA_DE_ASISTENCIA'),
  (10, 7, 9,  9,  '2026-2', 2.16, 'REPROBADO'),
  (11, 8, 10, 10, '2026-2', NULL, 'EN_CURSO'),
  (12, 1, 4,  3,  '2026-2', NULL, 'EN_CURSO'),
  (13, 1, 12, 1,  '2026-1', 3.20, 'APROBADO'),
  (14, 3, 13, 5,  '2026-1', 2.40, 'REPROBADO');

-- Controles de clase. Ana asistió a todo; David faltó 2 de 5 (60 %) y Camilo 3 de 4 (25 %): los dos
-- pierden el curso por inasistencia aunque la nota les alcance. Juan Esteban se queda justo en 75 %
-- (1 falta de 4), el límite donde todavía no se pierde.
INSERT INTO `asistencias` (`id_estudiante`, `id_seccion`, `fecha`, `estado`) VALUES
  (1, 1, '2026-08-03', 'ASISTIO'),
  (1, 1, '2026-08-10', 'ASISTIO'),
  (1, 1, '2026-08-17', 'TARDANZA'),
  (1, 1, '2026-08-24', 'ASISTIO'),
  (1, 1, '2026-08-31', 'ASISTIO'),
  (2, 1, '2026-08-03', 'ASISTIO'),
  (2, 1, '2026-08-10', 'FALTO'),
  (2, 1, '2026-08-17', 'ASISTIO'),
  (2, 1, '2026-08-24', 'FALTO'),
  (2, 1, '2026-08-31', 'ASISTIO'),
  (6, 8, '2026-08-04', 'FALTO'),
  (6, 8, '2026-08-11', 'FALTO'),
  (6, 8, '2026-08-18', 'FALTO'),
  (6, 8, '2026-08-25', 'ASISTIO'),
  (3, 5, '2026-08-05', 'ASISTIO'),
  (3, 5, '2026-08-12', 'JUSTIFICADO'),
  (4, 5, '2026-08-05', 'ASISTIO'),
  (4, 5, '2026-08-12', 'ASISTIO'),
  (4, 5, '2026-08-19', 'FALTO'),
  (4, 5, '2026-08-26', 'ASISTIO');

-- Notas del periodo activo. El promedio y el estado los puede recalcular la aplicación; los valores
-- de `estado` coinciden con las asistencias de arriba, y la nota de la matrícula con el promedio.
INSERT INTO `calificaciones` (`id_estudiante`, `id_seccion`, `nota_parcial1`, `nota_parcial2`, `nota_examen_final`, `estado`) VALUES
  (1, 1, 3.50, 4.00, 3.80, 'APROBADO'),
  (2, 1, 4.20, 3.90, 4.10, 'PERDIDO_POR_FALTA_DE_ASISTENCIA'),
  (3, 5, 3.00, 2.80, NULL, 'EN_CURSO'),
  (4, 5, 2.90, 3.40, 3.10, 'APROBADO'),
  (5, 7, 4.50, NULL, NULL, 'EN_CURSO'),
  (6, 8, 4.80, 4.60, 5.00, 'PERDIDO_POR_FALTA_DE_ASISTENCIA'),
  (7, 9, 2.10, 1.90, 2.40, 'REPROBADO');

-- Cierre de 2026-1: notas completadas, con la matrícula ya en su resultado definitivo.
INSERT INTO `calificaciones` (`id_estudiante`, `id_seccion`, `nota_parcial1`, `nota_parcial2`, `nota_examen_final`, `estado`) VALUES
  (1, 12, 3.00, 3.00, 3.50, 'APROBADO'),
  (3, 13, 2.00, 2.00, 3.00, 'REPROBADO');

-- Cuentas de acceso de la demo. El hash lo genera la propia aplicación, así que cree las cuentas DESPUÉS de
-- cargar esta semilla (el cuarto argumento exige un id_profesor o id_estudiante que ya exista). `compile`
-- delante porque target/classes aún no existe en un PC recién montado, y sin `-o` por el mismo motivo:
--
--   mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario -Dexec.args="admin.ut ClaveDemo123! ADMINISTRATIVO"
--   mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario -Dexec.args="profesor.cardozo ClaveDemo123! PROFESOR 1"
--   mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario -Dexec.args="estudiante.rojas ClaveDemo123! ESTUDIANTE 1"
--
-- (el último número es id_profesor o id_estudiante de las tablas de arriba).
