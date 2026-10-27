#!/usr/bin/env bash
# Monta en una base APARTE el volcado .sql que le envió un compañero y lo deja con el esquema que usa
# el proyecto (aplica 001 + 002 + 003 + 004 en orden). La base `universidad` real no se toca nunca.
#
#   cd UniversidadDao
#   MYSQL_BIN='/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe' \
#     bash scripts/adaptar-base-del-companero.sh ~/Downloads/universidad.sql root su-clave
#
# Argumentos: <volcado.sql> [base destino] [usuario MySQL] [contraseña]
#   base destino  por defecto universidad_companero (existe = aborta, salvo REINICIAR=1)
#   usuario       por defecto root
#   contraseña    por defecto la variable de entorno DB_PASSWORD o MYSQL_PWD
#
# Variables:
#   MYSQL_BIN     ruta del cliente mysql (en Windows suele estar en C:/Program Files/MySQL/...)
#   BASE          nombre de la base destino
#   REINICIAR=1   borra y vuelve a crear la base destino si ya existe (jamás con `universidad`)
#   FORZAR=1      permite usar como destino la base `universidad` (pierde lo que tenga ahora)
#
# Qué hace y por qué:
#   1. crea la base destino vacía.
#   2. carga el volcado sin sus líneas `CREATE DATABASE` / `USE`: el archivo del compañero apunta a su
#      propio `universidad` y hace `DROP TABLE` de todo; si se ejecutara tal cual sobre su equipo,
#      borraría la base real. Por eso el destino se fija con la opción -D del cliente.
#   3. aplica db/migrations/001 (las claves foráneas que protegen los datos), 002 (sección obligatoria
#      en la matrícula, controles únicos, CHECK y enums) y 003 (tablas sueltas y la forma antigua de
#      `usuarios`: columna `id_referencia` y el rol `ADMINISTRADOR`).
#   4. repasa las reglas con db/seed/comprobar-demo.sql y muestra el estado de la base.
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1
DUMP="${1:-}"
BASE="${BASE:-universidad_companero}"
DB_USER="${3:-${DB_USER:-root}}"
export MYSQL_PWD="${4:-${DB_PASSWORD:-${MYSQL_PWD:-}}}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-java}"

if [ -z "$DUMP" ] || [ ! -f "$DUMP" ]; then
  echo "Indique el archivo del compañero: bash scripts/adaptar-base-del-companero.sh ruta/al/volcado.sql" >&2
  exit 1
fi
if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
  echo "No se encontró el cliente mysql; indique la ruta con MYSQL_BIN:" >&2
  echo "  MYSQL_BIN='/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe' bash scripts/adaptar-base-del-companero.sh archivo.sql" >&2
  exit 1
fi
if [ "$BASE" = "universidad" ] && [ "${FORZAR:-}" != "1" ]; then
  echo "Se niega a escribir sobre la base real 'universidad'. Use otro nombre o confirme con FORZAR=1." >&2
  exit 1
fi

M() { "$MYSQL_BIN" -u "$DB_USER" -D "$BASE" --default-character-set=utf8mb4 "$@"; }
# Las migraciones y las comprobaciones también traen su `USE universidad;`; se quitan para que el
# destino sea el que fijó -D y no la base real.
sin_use() { sed -E '/^USE /d; /^CREATE DATABASE/d' "$1"; }

EXISTE=$(M -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$BASE';" 2>/dev/null || echo 0)
if [ "$EXISTE" != "0" ]; then
  if [ "${REINICIAR:-}" != "1" ]; then
    echo "La base $BASE ya existe y puede tener datos. Vacíela y créela de nuevo con REINICIAR=1." >&2
    exit 1
  fi
  echo "== 0. $BASE ya existía: se descarta y se recrea (REINICIAR=1)"
  M -e "DROP DATABASE IF EXISTS $BASE;" || exit 1
fi

echo "== 1. crear $BASE vacía"
"$MYSQL_BIN" -u "$DB_USER" --default-character-set=utf8mb4 -e \
  "CREATE DATABASE $BASE CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" || exit 1
"$MYSQL_BIN" -u "$DB_USER" --default-character-set=utf8mb4 -N -e "SELECT VERSION();" | head -1

echo "== 2. cargar el volcado del compañero"
sin_use "$DUMP" | M || exit 1

echo "== 3. migraciones 001, 002 y 003 sobre el volcado"
for m in 001_seguridad_y_fk 002_reglas_academicas 003_dump_de_un_companero 004_normalizar_datos_herados; do
  echo "   - $m"
  sin_use "db/migrations/$m.sql" | M || { echo "     falló $m; corrija el dato que señala MySQL y vuelva a lanzar el guion con REINICIAR=1." >&2; exit 1; }
done

echo "== 4. tablas que quedan (deben ser las once del proyecto)"
M -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='$BASE' ORDER BY table_name;"

echo "== 5. db/seed/comprobar-demo.sql (una base ajena suele dejar los históricos que solo arregla la app)"
sin_use db/seed/comprobar-demo.sql | M -t

echo "== 6. estado"
M -t -e "SELECT (SELECT COUNT(*) FROM facultades)   AS facultades,
                (SELECT COUNT(*) FROM carreras)     AS carreras,
                (SELECT COUNT(*) FROM estudiantes)  AS estudiantes,
                (SELECT COUNT(*) FROM cursos)       AS cursos,
                (SELECT COUNT(*) FROM secciones)    AS secciones,
                (SELECT COUNT(*) FROM matriculas)   AS matriculas,
                (SELECT COUNT(*) FROM matriculas WHERE id_seccion IS NULL) AS sin_seccion,
                (SELECT COUNT(*) FROM asistencias)  AS asistencias,
                (SELECT COUNT(*) FROM calificaciones) AS calificaciones,
                (SELECT COUNT(*) FROM usuarios)     AS cuentas;
         SELECT username, rol, id_estudiante, id_profesor, estado FROM usuarios;"

echo
echo "Base adaptada en $BASE. Para probarla sin mover nada más:"
echo "  DB_URL=\"jdbc:mysql://localhost:3306/$BASE\" DB_USER=$DB_USER DB_PASSWORD=... java -jar target/app.jar"
echo "Para entrar hace falta una cuenta con la clave que usted conozca (las del volcado vienen con la"
echo "clave del compañero; el hash está en el formato de PasswordHasher, así que sirve si la sabe):"
echo "  DB_URL=\"jdbc:mysql://localhost:3306/$BASE\" DB_USER=$DB_USER DB_PASSWORD=... \\"
echo "    java -cp target/app.jar universidad.seguridad.CrearUsuario su-admin SuClave1234567 ADMINISTRATIVO"
echo "Y cuando ya la tenga a prueba, copie la URL a src/main/resources/application-local.properties."
