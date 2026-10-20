-- Comprobaciones de la base de demostración. Todas las consultas deben devolver 0 filas.
-- Sirven después de cargar db/init/01-schema.sql + db/seed/datos-demo.sql, y también después de
-- migrar una base antigua con db/migrations/002_reglas_academicas.sql.
--
--   mysql -u root -p universidad < db/seed/comprobar-demo.sql
--
-- Repiten, en SQL, las reglas que aplican los servicios de universidad.servicio: si alguna falla,
-- el semilla o la migración dejaron datos que la aplicación no habría aceptado.
--
-- Sobre la base de demostración deben salir todas las consultas vacías. Sobre una base migrada con
-- 002 es normal que queden los dos históricos que solo la app resuelve: matrículas de otro curso
-- (comprobación 1) y estados sin recalcular (5 y 6). La aplicación los corrige al editar cada fila.

USE universidad;

-- 1 · Toda matrícula está en un curso de la carrera del estudiante (ServicioMatriculas).
SELECT 'matricula fuera de la carrera' AS fallo, COUNT(*) AS filas
  FROM matriculas m
  JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
  JOIN cursos c ON c.id_curso = m.id_curso
 WHERE c.id_carrera <> e.id_carrera
HAVING filas > 0;

-- 2 · El curso y el periodo de cada matrícula coinciden con los de su sección.
SELECT 'matricula contradice a su seccion' AS fallo, COUNT(*) AS filas
  FROM matriculas m
  JOIN secciones s ON s.id_seccion = m.id_seccion
  JOIN periodos_academicos p ON p.id_periodo = s.id_periodo
 WHERE m.id_curso <> s.id_curso OR m.periodo <> p.nombre
HAVING filas > 0;

-- 3 · Ninguna sección supera su cupo máximo.
SELECT 'seccion sobre su cupo' AS fallo, COUNT(*) AS filas
  FROM (SELECT s.id_seccion, s.cupo_maximo, COUNT(*) AS ocupados
          FROM secciones s JOIN matriculas m ON m.id_seccion = s.id_seccion
         GROUP BY s.id_seccion, s.cupo_maximo) g
 WHERE g.ocupados > g.cupo_maximo
HAVING filas > 0;

-- 4 · Nadie acumula más de 20 créditos en un mismo periodo.
SELECT 'creditos por encima del tope' AS fallo, COUNT(*) AS filas
  FROM (SELECT m.id_estudiante, m.periodo, SUM(c.creditos) AS creditos
          FROM matriculas m JOIN cursos c ON c.id_curso = m.id_curso
         GROUP BY m.id_estudiante, m.periodo) g
 WHERE g.creditos > 20
GROUP BY 1 HAVING filas > 0;

-- 5 · La nota de la matrícula es el promedio de la definitiva (30 % + 30 % + 40 %, dos decimales).
SELECT 'nota de matricula distinta del promedio' AS fallo, COUNT(*) AS filas
  FROM matriculas m
  JOIN calificaciones k ON k.id_estudiante = m.id_estudiante AND k.id_seccion = m.id_seccion
 WHERE k.nota_parcial1 IS NOT NULL AND k.nota_parcial2 IS NOT NULL AND k.nota_examen_final IS NOT NULL
   AND m.nota <> k.promedio_final
HAVING filas > 0;

-- 6 · El estado de la matrícula sigue la regla: primero la asistencia, luego la nota.
SELECT 'estado de matricula incorrecto' AS fallo, COUNT(*) AS filas
  FROM matriculas m
  LEFT JOIN (SELECT id_estudiante, id_seccion, COUNT(*) AS controles,
                    SUM(estado = 'FALTO') AS faltas
               FROM asistencias GROUP BY id_estudiante, id_seccion) a
         ON a.id_estudiante = m.id_estudiante AND a.id_seccion = m.id_seccion
 WHERE m.estado <> CASE
       WHEN a.controles >= 4 AND (a.controles - a.faltas) / a.controles < 0.75
           THEN 'PERDIDO_POR_FALTA_DE_ASISTENCIA'
       WHEN m.nota IS NULL THEN 'EN_CURSO'
       WHEN m.nota >= 3.0 THEN 'APROBADO'
       ELSE 'REPROBADO' END
HAVING filas > 0;

-- 7 · El estado de la calificación se deriva de sus notas (la pérdida por asistencia es la excepción).
SELECT 'estado de calificacion incorrecto' AS fallo, COUNT(*) AS filas
  FROM calificaciones k
 WHERE k.estado <> CASE
       WHEN k.nota_parcial1 IS NULL OR k.nota_parcial2 IS NULL OR k.nota_examen_final IS NULL
           THEN 'EN_CURSO'
       WHEN k.promedio_final >= 3.0 THEN 'APROBADO'
       ELSE 'REPROBADO' END
   AND k.estado <> 'PERDIDO_POR_FALTA_DE_ASISTENCIA'
HAVING filas > 0;

-- 8 · Nadie sin matrícula recibe lista o notas.
SELECT 'asistencia sin matricula' AS fallo, COUNT(*) AS filas
  FROM asistencias a
  LEFT JOIN matriculas m ON m.id_estudiante = a.id_estudiante AND m.id_seccion = a.id_seccion
 WHERE m.id_matricula IS NULL
HAVING filas > 0;

SELECT 'calificacion sin matricula' AS fallo, COUNT(*) AS filas
  FROM calificaciones k
  LEFT JOIN matriculas m ON m.id_estudiante = k.id_estudiante AND m.id_seccion = k.id_seccion
 WHERE m.id_matricula IS NULL
HAVING filas > 0;

-- 9 · Cada control de asistencia cae dentro de las fechas de su periodo.
SELECT 'asistencia fuera del periodo' AS fallo, COUNT(*) AS filas
  FROM asistencias a
  JOIN secciones s ON s.id_seccion = a.id_seccion
  JOIN periodos_academicos p ON p.id_periodo = s.id_periodo
 WHERE a.fecha NOT BETWEEN p.fecha_inicio AND p.fecha_fin
HAVING filas > 0;

-- 10 · Las cuentas no administrativas tienen su ficha (la regla que MySQL no deja poner en CHECK).
SELECT 'cuenta sin ficha' AS fallo, COUNT(*) AS filas
  FROM usuarios u
 WHERE (u.rol = 'ESTUDIANTE' AND u.id_estudiante IS NULL)
    OR (u.rol = 'PROFESOR' AND u.id_profesor IS NULL)
HAVING filas > 0;
