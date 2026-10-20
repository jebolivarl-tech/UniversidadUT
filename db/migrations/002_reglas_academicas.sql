-- 002 · Reglas académicas sobre una base `universidad` ya instalada.
-- No borra datos: agrega columnas, completa la sección de las matrículas existentes y convierte en
-- restricciones de base las reglas que hasta ahora solo vivían en Java.
--
-- Uso:
--   mysql -u root -p universidad < db/migrations/002_reglas_academicas.sql
-- Los ALTER se confirman solos (MySQL no los mete en transacción): si uno falla, corrija el dato
-- que lo provocó y vuelva a ejecutar el archivo desde ese paso.

USE universidad;

-- 1 · Matrícula: sección, estado, fecha de inscripción, y la nota y el periodo con las medidas reales
--     (dos decimales para la nota; el nombre del periodo entra completo, no truncado a 20).
ALTER TABLE `matriculas`
  ADD COLUMN `id_seccion` int NULL AFTER `id_estudiante`,
  MODIFY COLUMN `periodo` varchar(50) NOT NULL,
  MODIFY COLUMN `nota` decimal(4,2) NULL DEFAULT NULL,
  ADD COLUMN `estado` enum('EN_CURSO','APROBADO','REPROBADO','PERDIDO_POR_FALTA_DE_ASISTENCIA')
      NOT NULL DEFAULT 'EN_CURSO' AFTER `nota`,
  ADD COLUMN `fecha_matricula` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `estado`;

-- 2 · Casar las matrículas con su sección: curso y nombre del periodo, solo cuando el curso tiene
--     UN único grupo en ese periodo. Con dos grupos sería una asignación al azar y es el paso 3 el
--     que decide. El LIKE tolera los nombres que la base antigua guardó truncados a 20 caracteres.
UPDATE `matriculas` m
JOIN (SELECT s.`id_curso`, p.`nombre`, MIN(s.`id_seccion`) AS id_seccion
        FROM `secciones` s JOIN `periodos_academicos` p ON p.`id_periodo` = s.`id_periodo`
       GROUP BY s.`id_curso`, p.`nombre`
      HAVING COUNT(*) = 1) unico ON unico.`id_curso` = m.`id_curso`
                       AND (unico.`nombre` = m.`periodo` OR unico.`nombre` LIKE CONCAT(m.`periodo`, '%'))
SET m.`id_seccion` = unico.id_seccion
WHERE m.`id_seccion` IS NULL;

-- 2b · Si un curso solo tiene un grupo abierto en toda la historia, no hacía falta casar el periodo.
UPDATE `matriculas` m
JOIN (SELECT `id_curso`, MIN(`id_seccion`) AS id_seccion, COUNT(*) AS grupos
      FROM `secciones` GROUP BY `id_curso` HAVING COUNT(*) = 1) unico ON unico.id_curso = m.id_curso
SET m.`id_seccion` = unico.id_seccion
WHERE m.`id_seccion` IS NULL;

-- 3 · PAUSA. Estas matrículas no encontraron sección, o encontraron varios grupos posibles:
--     asígnela a mano o elimínelas.
--       SELECT m.id_matricula, m.id_estudiante, m.id_curso, m.periodo,
--              (SELECT GROUP_CONCAT(CONCAT(s.id_seccion, ' (', s.aula, ')'))
--                 FROM secciones s WHERE s.id_curso = m.id_curso) opciones
--         FROM matriculas m WHERE m.id_seccion IS NULL;
--       UPDATE matriculas m JOIN secciones s ON s.id_seccion = <id de la sección correcta>
--          SET m.id_seccion = s.id_seccion WHERE m.id_matricula = <id de la matrícula>;
--     El paso 4b vuelve a escribir el curso y el periodo desde la sección, así que basta con el id.
-- Si queda alguna sin asignar, el paso 4 falla al pasar `id_seccion` a NOT NULL ("Data truncated").

-- 4 · La sección pasa a ser obligatoria y un grupo con matriculados deja de poder borrarse.
ALTER TABLE `matriculas`
  MODIFY COLUMN `id_seccion` int NOT NULL,
  ADD CONSTRAINT `fk_matricula_seccion` FOREIGN KEY (`id_seccion`)
      REFERENCES `secciones` (`id_seccion`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `uq_matricula_seccion` UNIQUE (`id_estudiante`, `id_seccion`),
  ADD CONSTRAINT `chk_matricula_nota` CHECK (`nota` IS NULL OR (`nota` BETWEEN 0 AND 5));

-- 4b · Con la sección ya obligatoria, el curso y el periodo de cada matrícula se reescriben a partir
--      de ella: es la relación que usa la aplicación, y deja atrás los nombres truncados a 20
--      caracteres y los cursos que alguien apuntó a mano. Tras la pausa del paso 3 también alcanza
--      las matrículas que el operador asignó durante ella.
UPDATE `matriculas` m
JOIN `secciones` s ON s.`id_seccion` = m.`id_seccion`
JOIN `periodos_academicos` p ON p.`id_periodo` = s.`id_periodo`
SET m.`periodo` = p.`nombre`, m.`id_curso` = s.`id_curso`
WHERE m.`periodo` <> p.`nombre` OR m.`id_curso` <> s.`id_curso`;

-- 5 · Un control por estudiante, sección y día. Primero se quitan los repetidos, conservando el más antiguo.
DELETE a FROM `asistencias` a
JOIN `asistencias` b
  ON b.`id_estudiante` = a.`id_estudiante` AND b.`id_seccion` = a.`id_seccion`
 AND b.`fecha` = a.`fecha` AND b.`id_asistencia` < a.`id_asistencia`;

ALTER TABLE `asistencias`
  ADD CONSTRAINT `uq_asistencia_dia` UNIQUE (`id_estudiante`, `id_seccion`, `fecha`);

-- 6 · Grupos duplicados: el mismo curso, periodo y aula no puede abrirse dos veces.
--     Revíeles antes, si la base tiene secciones repetidas o sin cupo:
--       SELECT id_curso, id_periodo, aula, COUNT(*) FROM secciones
--        GROUP BY id_curso, id_periodo, aula HAVING COUNT(*) > 1;
UPDATE `secciones` SET `cupo_maximo` = 30 WHERE `cupo_maximo` IS NULL OR `cupo_maximo` < 1;

ALTER TABLE `secciones`
  MODIFY COLUMN `cupo_maximo` int NOT NULL DEFAULT 30,
  ADD CONSTRAINT `chk_seccion_cupo` CHECK (`cupo_maximo` BETWEEN 1 AND 200),
  ADD CONSTRAINT `uq_seccion_grupo` UNIQUE (`id_curso`, `id_periodo`, `aula`);

-- 7 · Calificaciones: cero y "sin digitar" dejan de ser lo mismo.
--     Los 0.00 que ya estaban digitados se conservan como ceros válidos; si en su base los ceros eran
--     "todavía sin nota" repase las filas completas antes de seguir:
--       SELECT id_calificacion, id_estudiante, id_seccion FROM calificaciones
--        WHERE nota_parcial1 = 0 AND nota_parcial2 = 0 AND nota_examen_final = 0;
ALTER TABLE `calificaciones`
  MODIFY COLUMN `nota_parcial1` decimal(4,2) NULL DEFAULT NULL,
  MODIFY COLUMN `nota_parcial2` decimal(4,2) NULL DEFAULT NULL,
  MODIFY COLUMN `nota_examen_final` decimal(4,2) NULL DEFAULT NULL,
  MODIFY COLUMN `estado` enum('APROBADO','REPROBADO','EN_CURSO','PERDIDO_POR_FALTA_DE_ASISTENCIA')
      NOT NULL DEFAULT 'EN_CURSO';

-- 7b · El promedio de MySQL debe coincidir con el que calcula Java (los vacíos valen 0).
ALTER TABLE `calificaciones` DROP COLUMN `promedio_final`;
ALTER TABLE `calificaciones`
  ADD COLUMN `promedio_final` decimal(4,2) GENERATED ALWAYS AS
      (round(coalesce(`nota_parcial1`, 0) * 0.3 + coalesce(`nota_parcial2`, 0) * 0.3
             + coalesce(`nota_examen_final`, 0) * 0.4, 2)) STORED AFTER `nota_examen_final`;

ALTER TABLE `calificaciones`
  ADD CONSTRAINT `chk_calificacion_escala` CHECK (
      (`nota_parcial1` IS NULL OR `nota_parcial1` BETWEEN 0 AND 5)
      AND (`nota_parcial2` IS NULL OR `nota_parcial2` BETWEEN 0 AND 5)
      AND (`nota_examen_final` IS NULL OR `nota_examen_final` BETWEEN 0 AND 5));

-- 8 · Nombres únicos donde se usan como referencia: periodos, carreras, facultades.
--     Es frecuente que una base antigua tenga dos veces la misma facultad. Revíselos y fusione cada
--     duplicado antes de seguir: las claves foráneas no dejan borrar una facultad con carreras encima.
--       SELECT nombre, GROUP_CONCAT(id_facultad) ids FROM facultades GROUP BY nombre HAVING COUNT(*) > 1;
--       SELECT nombre, GROUP_CONCAT(id_carrera)  ids FROM carreras  GROUP BY nombre HAVING COUNT(*) > 1;
--       SELECT nombre, GROUP_CONCAT(id_periodo)   ids FROM periodos_academicos GROUP BY nombre HAVING COUNT(*) > 1;
--       UPDATE carreras SET id_facultad = <id que se conserva> WHERE id_facultad = <id duplicado>;
--       DELETE FROM facultades WHERE id_facultad = <id duplicado>;
--     Con los periodos duplicados hay que decidir cuál era el bueno: revise también secciones y
--     matrículas apuntando al id que elimina.
ALTER TABLE `periodos_academicos`
  MODIFY COLUMN `estado` enum('ACTIVO','INACTIVO','CERRADO') NOT NULL DEFAULT 'INACTIVO',
  ADD CONSTRAINT `uq_periodo_nombre` UNIQUE (`nombre`),
  ADD CONSTRAINT `chk_periodo_fechas` CHECK (`fecha_fin` >= `fecha_inicio`);

ALTER TABLE `carreras` ADD CONSTRAINT `uq_carrera_nombre` UNIQUE (`nombre`);
ALTER TABLE `facultades` ADD CONSTRAINT `uq_facultad_nombre` UNIQUE (`nombre`);
ALTER TABLE `cursos` ADD CONSTRAINT `chk_curso_creditos` CHECK (`creditos` BETWEEN 1 AND 10);

-- 9 · Una cuenta sin ficha vinculada no es de estudiante ni de profesor.
--     Esta regla NO puede convertirse en CHECK: MySQL la rechaza (error 3823) porque `id_estudiante`
--     e `id_profesor` ya participan en las claves foráneas de `usuarios`. La aplican
--     Validaciones.esCuentaValida en la API y universidad.seguridad.CrearUsuario en la consola,
--     que es además el único camino para crear cuentas con hash PBKDF2.
--     Verificación de cuentas huérfanas tras la migración:
--       SELECT username, rol FROM usuarios
--        WHERE (rol = 'ESTUDIANTE' AND id_estudiante IS NULL)
--           OR (rol = 'PROFESOR'    AND id_profesor    IS NULL);

-- 10 · El estado de las matrículas existentes se recalcula con la regla de asistencia en la primera
--      edición de cada una; no se toca por SQL para no pisar notas digitadas a mano.
--      Si prefiere cerrar ya el círculo entre calificaciones y matrículas —solo donde la definitiva
--      está completa y la nota difiere— descomente:
--        UPDATE matriculas m JOIN calificaciones k
--               ON k.id_estudiante = m.id_estudiante AND k.id_seccion = m.id_seccion
--           SET m.nota = k.promedio_final
--         WHERE k.nota_parcial1 IS NOT NULL AND k.nota_parcial2 IS NOT NULL
--           AND k.nota_examen_final IS NOT NULL AND m.nota <> k.promedio_final;
--
-- 11 · Compruebe el resultado con db/seed/comprobar-demo.sql (cambia el USE a su base). En una base
--      migrada es normal que sigan saliendo dos cosas que solo la aplicación puede juzgar: matrículas
--      de estudiantes inscritos en un curso de otra carrera (histórico) y estados EN_CURSO con nota
--      ya puesta, que se corregirán al editar cada matrícula.
