#!/usr/bin/env bash
# Recorre completa la ruta de migración sobre una base de pruebas, sin tocar `universidad`.
#
#   cd UniversidadDao
#   export MYSQL_PWD='la-contrasena-local-de-mi-mysql'    # si prefiere no escribirla a cada paso
#   bash scripts/verificar-migracion.sh [usuario]         # usuario de MySQL, por defecto root
#
# Qué hace:
#   1. crea `universidad_migracion_prueba` y la llena con los volcados antiguos de tablas-MySql/
#   2. inserta datos con la forma de antes: matrículas sin sección, periodo truncado a 20 caracteres,
#      asistencias repetidas el mismo día, notas 0.00, un cupo nulo y una facultad duplicada
#   3. aplica db/migrations/001 y 002 siguiendo el flujo real: la pausa del paso 3 aborta el 002
#   4. corrige a mano lo que la pausa y el paso 8 piden, y reanuda 002 desde el paso 4
#   5. ejecuta db/seed/comprobar-demo.sql sobre la base migrada y muestra lo que quedó pendiente
#   6. borra la base de pruebas
#
# En el paso 5 es normal que sigan saliendo los históricos que solo la aplicación resuelve: matrículas
# de un curso de otra carrera y estados que se recalculan al editar cada matrícula (paso 11 del 002).
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1
BASE="universidad_migracion_prueba"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
DB_USER="${1:-${DB_USER:-root}}"
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT
M() { "$MYSQL_BIN" -u "$DB_USER" --default-character-set=utf8mb4 "$@"; }

if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
  echo "No se encontró el cliente mysql; indique la ruta con MYSQL_BIN:" >&2
  echo "  MYSQL_BIN='/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe' bash scripts/verificar-migracion.sh" >&2
  exit 1
fi

echo "== 1. base de pruebas $BASE con los volcados antiguos"
M -e "DROP DATABASE IF EXISTS $BASE; CREATE DATABASE $BASE CHARACTER SET utf8mb4;" || exit 1
for f in tablas-MySql/*.sql; do
  M "$BASE" < "$f" || exit 1
done

echo "== 2. datos con la forma antigua"
sed "s/universidad_migracion_prueba/$BASE/g" > "$T/datos.sql" <<'EOF'
USE universidad_migracion_prueba;
SET @p  := (SELECT nombre FROM periodos_academicos WHERE id_periodo = 1);
SET @e1 := (SELECT MIN(id_estudiante) FROM estudiantes);
SET @e2 := (SELECT MIN(id_estudiante) FROM estudiantes WHERE id_estudiante > @e1);

INSERT INTO cursos (id_curso,nombre,creditos,id_carrera,id_profesor) VALUES
  (901,'Cálculo Diferencial (prueba)',4,1,1),
  (902,'Álgebra Lineal (prueba)',3,1,1),
  (903,'Estática (prueba)',3,1,1);

-- El 903 se abre en dos aulas del mismo periodo: su matrícula queda ambigua a propósito, que es justo
-- lo que detiene el 002 en el paso 4.
INSERT INTO secciones (id_seccion,id_curso,id_profesor,id_periodo,aula,cupo_maximo) VALUES
  (901,901,1,1,'B-101',30),(902,902,1,1,'B-102',25),
  (903,903,1,1,'A-204',NULL),(904,903,1,1,'A-205',20);

-- periodo guardado truncado a 20 caracteres, como lo escribía el esquema viejo.
INSERT INTO matriculas (id_estudiante,id_curso,periodo,nota) VALUES
  (@e1,901,LEFT(@p,20),3.8),(@e1,902,LEFT(@p,20),NULL),(@e2,901,LEFT(@p,20),4.1),(@e2,903,LEFT(@p,20),2.55);

INSERT INTO asistencias (id_estudiante,id_seccion,fecha,estado) VALUES
  (@e1,901,'2026-08-03','ASISTIO'),(@e1,901,'2026-08-03','TARDANZA'),(@e1,901,'2026-08-10','ASISTIO'),
  (@e2,901,'2026-08-03','FALTO'),(@e2,901,'2026-08-10','FALTO'),(@e2,901,'2026-08-17','FALTO'),
  (@e2,901,'2026-08-24','ASISTIO'),(@e2,901,'2026-08-24','FALTO');

INSERT INTO calificaciones (id_estudiante,id_seccion,nota_parcial1,nota_parcial2,nota_examen_final,estado) VALUES
  (@e1,901,3.50,4.00,3.80,'APROBADO'),(@e2,901,0.00,0.00,0.00,'REPROBADO');
EOF
M < "$T/datos.sql" || exit 1

echo "== 3. migración 001"
M "$BASE" < db/migrations/001_seguridad_y_fk.sql || exit 1

echo "== 4. migración 002 (debe parar en el paso 4)"
sed "s/\buniversidad\b/$BASE/g" db/migrations/002_reglas_academicas.sql > "$T/002.sql"
M < "$T/002.sql" > "$T/002.log" 2>&1
grep -E "ERROR" "$T/002.log" | head -3
echo "   matrículas que la pausa dejó sin sección:"
M "$BASE" -N -e "SELECT id_matricula,id_estudiante,id_curso,periodo FROM matriculas WHERE id_seccion IS NULL;"

echo "== 5. lo que resuelve el operador antes de reanudar"
M "$BASE" -e "
-- 5a · asigna la sección ambigua (el paso 3 del 002 deja esta misma consulta con ids concretos)
UPDATE matriculas m
  JOIN (SELECT id_curso, MIN(id_seccion) AS id_seccion FROM secciones GROUP BY id_curso) s
    ON s.id_curso = m.id_curso
   SET m.id_seccion = s.id_seccion
 WHERE m.id_seccion IS NULL;
-- 5b · funde las facultades repetidas (revisión pedida por el paso 8 del 002)
UPDATE carreras c
  JOIN facultades f ON f.id_facultad = c.id_facultad
  JOIN (SELECT nombre, MIN(id_facultad) conserva FROM facultades
         GROUP BY nombre HAVING COUNT(*) > 1) d ON d.nombre = f.nombre
   SET c.id_facultad = d.conserva
 WHERE c.id_facultad <> d.conserva;
UPDATE profesores p
  JOIN facultades f ON f.id_facultad = p.id_facultad
  JOIN (SELECT nombre, MIN(id_facultad) conserva FROM facultades
         GROUP BY nombre HAVING COUNT(*) > 1) d ON d.nombre = f.nombre
   SET p.id_facultad = d.conserva
 WHERE p.id_facultad <> d.conserva;
DELETE f FROM facultades f
  JOIN (SELECT nombre, MIN(id_facultad) conserva FROM facultades
         GROUP BY nombre HAVING COUNT(*) > 1) d ON d.nombre = f.nombre
 WHERE f.id_facultad <> d.conserva;" || exit 1

{ echo "USE $BASE;"; awk '/^-- 4 ·/{f=1} f' "$T/002.sql"; } > "$T/002-desde-4.sql"
echo "== 6. reanudar 002 desde el paso 4"
M < "$T/002-desde-4.sql" || exit 1

echo "== 7. estado de la base migrada"
M "$BASE" -t -e "
SELECT m.id_matricula,m.id_estudiante,m.id_seccion,m.id_curso,m.periodo,m.nota,m.estado
  FROM matriculas m ORDER BY m.id_matricula;
SELECT COUNT(*) asistencias_repetidas FROM (
  SELECT id_estudiante,id_seccion,fecha FROM asistencias GROUP BY 1,2,3 HAVING COUNT(*)>1) d;
SELECT id_seccion,aula,cupo_maximo FROM secciones WHERE id_seccion IN (901,902,903,904);
SELECT COUNT(*) facultades_repetidas FROM (SELECT nombre FROM facultades GROUP BY nombre HAVING COUNT(*)>1) f;"

echo "== 8. db/seed/comprobar-demo.sql sobre la base migrada"
sed "s/\buniversidad\b/$BASE/g" db/seed/comprobar-demo.sql > "$T/check.sql"
M -t < "$T/check.sql"

echo "== 9. limpieza"
M -e "DROP DATABASE IF EXISTS $BASE;"
echo "Listo: $BASE eliminada. La base 'universidad' no se tocó."
