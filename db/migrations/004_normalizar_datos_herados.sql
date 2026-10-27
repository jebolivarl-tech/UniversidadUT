-- 004 · Poner al día los datos que llegan de una base ajena.
--
-- Va después de 001, 002 y 003. No cambia el esquema: arregla lo que el volcado del compañero escribió
-- a mano antes de que estas reglas vivieran en la base. Cada sentencia es idempotente, así que se puede
-- repetir cuando herede otra carga.
--
--   mysql -u root -p universidad_companero < db/migrations/004_normalizar_datos_herados.sql
--   (o scripts/adaptar-base-del-companero.sh, que la aplica sola)
--
-- Al terminar, db/seed/comprobar-demo.sql debe salir vacío. Sobre la semilla del propio proyecto nunca
-- hace falta: ya se genera cumpliendo las reglas.

-- 1 · Una calificación implica matrícula: el compañero anotó notas de estudiantes que nunca quedaron
--     inscritos en el grupo. Sin esta fila, `mi.html` muestra notas de un curso donde el estudiante
--     no está, y la regla 8 de comprobar-demo.sql se incumpliría el resto de la vida.
INSERT INTO `matriculas` (`id_estudiante`, `id_seccion`, `id_curso`, `periodo`, `nota`, `estado`)
SELECT k.`id_estudiante`, k.`id_seccion`, s.`id_curso`, p.`nombre`, k.`promedio_final`, 'EN_CURSO'
  FROM `calificaciones` k
  JOIN `secciones` s ON s.`id_seccion` = k.`id_seccion`
  JOIN `periodos_academicos` p ON p.`id_periodo` = s.`id_periodo`
  LEFT JOIN `matriculas` m ON m.`id_estudiante` = k.`id_estudiante` AND m.`id_seccion` = k.`id_seccion`
 WHERE m.`id_matricula` IS NULL;

-- 2 · Las notas heredadas venían con un solo decimal (4.2) y ahora se publican con dos; la definitiva
--     es la que calcula MySQL, no la que alguien copió en la matrícula.
UPDATE `matriculas` m
  JOIN `calificaciones` k ON k.`id_estudiante` = m.`id_estudiante` AND k.`id_seccion` = m.`id_seccion`
 SET m.`nota` = k.`promedio_final`
 WHERE k.`nota_parcial1` IS NOT NULL AND k.`nota_parcial2` IS NOT NULL
   AND k.`nota_examen_final` IS NOT NULL AND m.`nota` <> k.`promedio_final`;

-- 3 · El estado de la matrícula era una columna que no existía: 002 la creó en EN_CURSO para todas.
--     Se deriva con la misma regla de `ServicioCalificaciones`: primero la asistencia (con cuatro o más
--     llamados, por debajo del 75 % se pierde el curso) y después la nota (se aprueba con 3.0).
UPDATE `matriculas` m
  LEFT JOIN (SELECT `id_estudiante`, `id_seccion`, COUNT(*) AS controles,
                    SUM(`estado` = 'FALTO') AS faltas
               FROM `asistencias` GROUP BY `id_estudiante`, `id_seccion`) a
         ON a.`id_estudiante` = m.`id_estudiante` AND a.`id_seccion` = m.`id_seccion`
 SET m.`estado` = CASE
       WHEN a.controles >= 4 AND (a.controles - a.faltas) / a.controles < 0.75
           THEN 'PERDIDO_POR_FALTA_DE_ASISTENCIA'
       WHEN m.`nota` IS NULL THEN 'EN_CURSO'
       WHEN m.`nota` >= 3.0 THEN 'APROBADO'
       ELSE 'REPROBADO' END
 WHERE m.`estado` <> CASE
       WHEN a.controles >= 4 AND (a.controles - a.faltas) / a.controles < 0.75
           THEN 'PERDIDO_POR_FALTA_DE_ASISTENCIA'
       WHEN m.`nota` IS NULL THEN 'EN_CURSO'
       WHEN m.`nota` >= 3.0 THEN 'APROBADO'
       ELSE 'REPROBADO' END;

-- 4 · `fecha_matricula`: 002 la llenó con la fecha de la migración porque el volcado no trae la de
--     inscripción. Se deja tal cual; inventar una fecha anterior sería peor dato que el que hay.
--     Las filas que insertó el paso 1 sí llevan la fecha correcta, la de hoy.

-- 5 · Lo que esta migración NO toca y solo la aplicación puede juzgar:
--     una matrícula heredada de un estudiante inscrito en un curso de otra carrera es histórica
--     (comprobación 1 de comprobar-demo.sql) y queda para revisión manual del administrativo.
