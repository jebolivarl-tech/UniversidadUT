#!/usr/bin/env bash
# Deja lista una base de pruebas aparte (`universidad_e2e`) para la verificación end-to-end.
# Nunca toca la base real `universidad`: la crea desde cero, la siembra y le crea tres cuentas.
#
#   cd UniversidadDao
#   bash scripts/preparar-base-pruebas.sh [usuario] [contrasena]   # por defecto root y MYSQL_PWD
#
# Variables que puede cambiar sin editar el archivo:
#   MYSQL_BIN  ruta del cliente mysql          BASE       nombre de la base de pruebas (universidad_e2e)
#   MYSQL_PWD  contraseña del usuario de MySQL (la leen el cliente y la aplicación)
#
# Qué hace:
#   1. bota y recrea la base a partir de db/init/01-schema.sql
#   2. carga db/seed/datos-demo.sql
#   3. ejecuta db/seed/comprobar-demo.sql (debe salir vacío: la semilla cumple las reglas)
#   4. crea las cuentas que usan las pruebas: e2e_admin, e2e_prof (profesor 3) y e2e_ana (estudiante 1)
#   5. muestra el estado de la base
#
# Después se arranca la aplicación contra esa base y se corre scripts/verificar-api-e2e.sh:
#   DB_URL="jdbc:mysql://localhost:3306/universidad_e2e" DB_USER=root DB_PASSWORD=su-clave java -jar target/app.jar
# Al terminar, cuando ya no sirva, se quita con:
#   mysql -u root -e "DROP DATABASE universidad_e2e"
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1
BASE="${BASE:-universidad_e2e}"
# El guion empieza con DROP DATABASE: si alguien apunta BASE a la base real, se queda sin datos.
if [ "$BASE" = "universidad" ] && [ "${FORZAR:-0}" != "1" ]; then
  echo "Se niega a trabajar sobre la base real 'universidad'. Use otra (p. ej. universidad_e2e)" >&2
  echo "o, si de verdad sabe lo que hace, repita con FORZAR=1." >&2
  exit 1
fi
MYSQL_BIN="${MYSQL_BIN:-mysql}"
DB_USER="${1:-${DB_USER:-root}}"
# La contraseña viaja por el entorno que lee el propio cliente: así no aparece en el comando ni en el log.
export MYSQL_PWD="${2:-${DB_PASSWORD:-${MYSQL_PWD:-}}}"

if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
  echo "No se encontró el cliente mysql; indique la ruta con MYSQL_BIN:" >&2
  echo "  MYSQL_BIN='/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe' bash scripts/preparar-base-pruebas.sh" >&2
  exit 1
fi
if [ ! -f target/app.jar ]; then
  echo "Falta target/app.jar: construya primero con  mvn -o clean package" >&2
  exit 1
fi

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-java}"
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT

M() { "$MYSQL_BIN" -u "$DB_USER" --default-character-set=utf8mb4 "$@"; }
# El volcado real apunta a `universidad`; se reescribe el nombre para que caiga en la base de pruebas.
cargar() { sed -E "s/\buniversidad\b/$BASE/g" "$1" > "$T/$(basename "$1")"; }

echo "== 1. base de pruebas $BASE desde db/init/01-schema.sql"
M -e "DROP DATABASE IF EXISTS $BASE; CREATE DATABASE $BASE CHARACTER SET utf8mb4;" || exit 1
cargar db/init/01-schema.sql && M < "$T/01-schema.sql" || exit 1

echo "== 2. semilla de demostración"
cargar db/seed/datos-demo.sql && M < "$T/datos-demo.sql" || exit 1

echo "== 3. db/seed/comprobar-demo.sql (vacío = la semilla cumple las reglas)"
cargar db/seed/comprobar-demo.sql && M -t < "$T/comprobar-demo.sql"

echo "== 4. cuentas de acceso para las pruebas"
export DB_URL="jdbc:mysql://localhost:3306/$BASE" DB_USER="$DB_USER" DB_PASSWORD="$MYSQL_PWD"
"$JAVA_BIN" -cp target/app.jar universidad.seguridad.CrearUsuario e2e_admin ClaveAdmin123   ADMINISTRATIVO || exit 1
"$JAVA_BIN" -cp target/app.jar universidad.seguridad.CrearUsuario e2e_prof  ClaveProfesor1   PROFESOR 3     || exit 1
"$JAVA_BIN" -cp target/app.jar universidad.seguridad.CrearUsuario e2e_ana   ClaveEstudiante1 ESTUDIANTE 1   || exit 1

echo "== 5. estado de la base"
M -t -e "USE $BASE; SELECT rol, COUNT(*) AS cuentas FROM usuarios GROUP BY rol;
SELECT (SELECT COUNT(*) FROM estudiantes)   AS estudiantes,
       (SELECT COUNT(*) FROM secciones)     AS secciones,
       (SELECT COUNT(*) FROM matriculas)    AS matriculas,
       (SELECT COUNT(*) FROM asistencias)   AS asistencias,
       (SELECT COUNT(*) FROM calificaciones) AS calificaciones;"

echo
echo "Base lista. Arranque la aplicación contra $BASE y ejecute scripts/verificar-api-e2e.sh"
