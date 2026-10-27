# Instalación paso a paso (solo localhost)

Guía para montar el proyecto en un equipo desde cero. Está pensada para Windows, pero los comandos de
Maven y MySQL son los mismos en Linux o macOS.

## Camino corto: `instalar-pc.bat`

En Windows, los pasos 2 a 6 de esta guía están automatizados en `instalar-pc.bat` (doble clic, o desde una
consola dentro de la carpeta del proyecto). Comprueba JDK, Maven y `mysql.exe` —busca Maven también donde
lo dejan los IDEs—, verifica que la versión de MySQL admita el esquema, crea la base `universidad`, ofrece
la semilla de demostración, escribe `src/main/resources/application-local.properties` con las credenciales
que usted conteste, crea su cuenta `ADMINISTRATIVO` y termina con `mvn clean package`. No borra una base
que ya exista sin preguntarlo, y no sube nada a git.

Se puede gobernar sin preguntas con variables de entorno (útil para repetirlo en varios equipos):
`DB_NOMBRE`, `DB_USER`, `DB_PASSWORD`, `CARGAR_DEMO=S|N`, `CREAR_CUENTA=S|N`, `ADMIN_USUARIO`,
`ADMIN_CLAVE`, y para las herramientas fuera del `PATH`, `MYSQL_BIN` y `MAVEN_BIN`.

Si algo falla, el guion se detiene con el motivo y este documento explica el resto.

## 0. Qué necesita tener instalado

| Herramienta | Versión mínima | Para qué |
|---|---|---|
| JDK | 17 | Compilar y ejecutar (el `pom.xml` fija `--release 17`). |
| Maven | 3.8 | Construir el `target/app.jar`. |
| MySQL | **8.0.16** | La base `universidad`, corriendo en `localhost:3306`. |

Los tres tienen que verse desde la terminal. En Windows, si instaló el JDK o Maven desde un IDE, añada sus
carpetas `bin` a la variable `PATH` (o trabaje desde la terminal de ese IDE). Compruebe:

```bash
java -version    # openjdk 17 o superior
mvn -v
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p -e "SELECT VERSION();"
```

El requisito de MySQL no es un capricho: `db/init/01-schema.sql` usa la colación `utf8mb4_0900_ai_ci` y
restricciones `CHECK`. MySQL 5.7 y MariaDB lo rechazan con `Unknown collation`; los `CHECK` además
solo se aplican desde 8.0.16.

Y necesitará **internet en el primer build**: Maven baja unas 52 MB de dependencias a `~/.m2`. Hasta que eso
no esté en el equipo, no use `-o` (el paso 5 y el 6 lo explican).

## 1. Conseguir el código

```bash
git clone https://github.com/jebolivarl-tech/UniversidadUT.git UniversidadDao
cd UniversidadDao
```

Si prefiere el ZIP, descomprímalo y entre en la carpeta `UniversidadDao-corregido`.

### Y si lo abren en VS Code

El que compila y ejecuta es Maven, no el IDE: los comandos de esta guía son los mismos desde la terminal
integrada de VS Code que desde `cmd`. Con `Extension Pack for Java`, VS Code baja su propio Maven a
`%USERPROFILE%\.m2\wrapper\...\bin\mvn.cmd`, y `instalar-pc.bat` busca también ahí, así que no hace falta
añadir nada al `PATH`.

Mientras no hayan compilado, el editor marcará en rojo los `import` de Javalin, `mysql-connector-j` o
`AbsoluteLayout`: no es un error del código, es que `~/.m2` todavía no tiene las dependencias. Se resuelve
con `instalar-pc.bat` o con `mvn clean package`.

## 2. Crear la base de datos

**Opción A — base nueva (recomendado):** el esquema crea la base, las 11 tablas, las claves foráneas,
los `CHECK`, las claves únicas y la columna generada `promedio_final`.

```bash
mysql -u root -p < db/init/01-schema.sql
```

**Opción B — ya tiene datos de un curso anterior:** no ejecute el esquema (hace `DROP TABLE`). Aplique las
migraciones numeradas, en orden:

```bash
mysql -u root -p universidad < db/migrations/001_seguridad_y_fk.sql
mysql -u root -p universidad < db/migrations/002_reglas_academicas.sql
```

El paso 3 de `002` está hecho a propósito para detenerse: deja una pausa con las consultas para
asignar a mano las matrículas que no encuentran sección. Corrija lo que pida y vuelva a ejecutar el
archivo desde el paso que falló. Al terminar, compruebe el resultado:

```bash
mysql -u root -p < db/seed/comprobar-demo.sql   # cambie el USE a su base si no es `universidad`
```

### Si le llega el `universidad.sql` de un compañero

Un `mysqldump` ajeno llega con su propio `CREATE DATABASE universidad` y con `DROP TABLE` delante de
cada tabla: ejecutarlo tal cual sobre su equipo **borra la base que ya tiene**. Además suele venir del
esquema antiguo (`matriculas` sin `id_seccion`, `usuarios` con `id_referencia` y el rol
`ADMINISTRADOR`, y tablas sueltas tipo `libro1` traídas de un Excel).

Súbalo a una base aparte y déjelo listo de una sola pasada:

```bash
MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" \
  bash scripts/adaptar-base-del-companero.sh ~/Downloads/universidad.sql universidad_companero root su-clave
```

El guion quita las líneas que apuntan a `universidad`, crea la base destino, carga el volcado y aplica
`001` → `002` → `003_dump_de_un_companero.sql` (las once tablas y la forma actual de `usuarios`) →
`004_normalizar_datos_herados.sql` (la matrícula que falta donde hay notas, la definitiva de dos
decimales y el estado derivado de asistencia y nota). Termina con `comprobar-demo.sql`: sobre un volcado
adaptado debe salir **vacío**; si queda alguna matrícula de un curso que no es de la carrera del
estudiante, es histórico y lo revisa el administrativo desde la web.

Después, o se trabaja contra esa base (ponga su `DB_URL` en `application-local.properties`, paso 4) o se
pasan sus registros a la del grupo. En el segundo caso, los ids del volcado y los de su base chocan:
lo ordenado es heredar hacia la base adaptada, desplazando sus ids (`id + 100`) y emparejando carreras y
facultades por nombre, como hace este bloque:

```sql
INSERT INTO universidad_companero.estudiantes
      (id_estudiante, nombre, apellido, email, fecha_nacimiento, id_carrera)
SELECT 100 + e.id_estudiante, e.nombre, e.apellido, e.email, e.fecha_nacimiento, c.id_carrera
  FROM universidad.estudiantes e
  JOIN universidad.carreras oc          ON oc.id_carrera = e.id_carrera
  JOIN universidad_companero.carreras c ON c.nombre = oc.nombre
 WHERE NOT EXISTS (SELECT 1 FROM universidad_companero.estudiantes t WHERE t.email = e.email);
```

Las cuentas viajan con su hash (el formato `pbkdf2_sha256$iteraciones$salt$hash` es el mismo que lee
`PasswordHasher`), así que cada persona sigue entrando con la clave que ya tenía. Repita el bloque con
`profesores`, `periodos_academicos` y `usuarios`; compruebe al final que no queda ninguna cuenta sin
ficha, que es lo que rechaza `Validaciones.esCuentaValida`.

**Paso final: volcar la base adaptada sobre `universidad`.** Es lo que hace que la web muestre ya los
datos del grupo, porque `DB_URL` apunta a `universidad`. Respalde primero y cargue después; el volcado
no lleva `CREATE DATABASE` ni `USE`, así que el destino lo fija el `-D` del cliente:

```bash
mysqldump -u root --single-transaction --routines --triggers --databases universidad \
  > respaldo-universidad-antes-del-volcado.sql      # 1. respaldo, compruebe que trae las 11 tablas
mysqldump -u root --single-transaction --routines --triggers universidad_companero \
  | mysql -u root -D universidad                    # 2. sustituye las 11 tablas por las adaptadas
sed -E '/^USE /d' db/seed/comprobar-demo.sql | mysql -u root -D universidad -t   # 3. vacío = correcto
```

Para deshacer: `mysql -u root < respaldo-universidad-antes-del-volcado.sql`. MySQL no tiene
`RENAME DATABASE`, por eso el intercambio se hace con volcado y carga, y conviene dejar
`universidad_companero` intacta hasta revisar la web con las cuentas reales.

## 3. Datos de demostración (opcional)

Para ver el sistema funcionando sin cargar registros uno por uno:

```bash
mysql -u root -p universidad < db/seed/datos-demo.sql
```

Incluye 5 facultades, 7 carreras, 8 estudiantes, 13 secciones, matrículas con notas y casos reales de
pérdida por asistencia. **No lo ejecute sobre una base con datos.** Las cuentas de acceso se crean en
el paso 5.

## 4. Credenciales de la base

Al clonar, este archivo **no existe**: está en `.gitignore` y por eso nunca sube al repositorio. Copie la
plantilla (en el CMD de Windows es `copy`, no `cp`) y escriba su contraseña local de MySQL:

```bash
cp application-local.properties.example src/main/resources/application-local.properties
```

```properties
DB_URL=jdbc:mysql://localhost:3306/universidad
DB_USER=su-usuario
DB_PASSWORD=la-suya
```

`DB_URL` es opcional (sin ella se usa la URL de arriba). Las variables de entorno `DB_USER` y `DB_PASSWORD`
tienen prioridad sobre el archivo, y `DB_POOL_SIZE` ajusta el tamaño del pool. **Nunca suba contraseñas al
repositorio.** Si omite este paso, `run-app.bat` avisa y sale sin intentar arrancar.

## 5. Primeras cuentas de acceso

La base nace **sin usuarios**: con el esquema y la semilla cargados todavía no hay con quién entrar, y las
contraseñas se guardan con hash PBKDF2. El cuarto argumento es la ficha del rol: `id_estudiante` para
ESTUDIANTE, `id_profesor` para PROFESOR, y se omite en ADMINISTRATIVO. La contraseña mínima son 10
caracteres (`seguridad/CrearUsuario.java:28`).

Estos comandos llevan `compile` delante y van **sin `-o`**: en un PC recién montado `target/classes` aún no
existe (el goal `exec:java` no compila por su cuenta) y Maven todavía no tiene las dependencias en `~/.m2`.

```bash
mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="admin UnaClaveLarga123 ADMINISTRATIVO"

mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="profesor.cardozo OtraClaveLarga123 PROFESOR 1"

mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="estudiante.rojas ClaveDeAna2026 ESTUDIANTE 1"
```

Si viene de una base antigua con contraseñas en texto plano:
`mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords` (el primer login también
convierte la cuenta usada).

## 6. Compilar y probar

```bash
mvn clean test       # 77 pruebas: reglas académicas, validaciones, sesión, JSON
mvn clean package    # genera target/app.jar; pare el servidor antes, o la JVM tiene el jar bloqueado
```

Con las dependencias ya descargadas se puede trabajar sin red añadiendo `-o` (`mvn -o clean test`). El
`AbsoluteLayout-SNAPSHOT.jar` que usa el `pom.xml` viene dentro del repositorio (`lib/`), así que no hay
que buscarlo: Maven lo toma de ese repositorio local y solo imprime un aviso por su POM ausente.

## 7. Arrancar la interfaz web

```bash
java -jar target/app.jar     # o: run-app.bat
```

- Inicio de sesión: <http://localhost:8080/login.html>
- Panel: <http://localhost:8080/menu.html>
- Diagnóstico: <http://localhost:8080/api/health> debe responder `{"status":"UP","database":"UP"}`

El puerto se cambia con la variable de entorno `PORT`. Si aparece `JavalinBindException: Port already
in use`, hay otra instancia levantada: cierre la anterior o use otro `PORT`.

## 8. Arrancar el cliente de escritorio

Con la misma cuenta y la misma base, sin pasar por la API:

```bash
java -cp target/app.jar universidad.App
```

## 9. Comprobar que las reglas funcionan

Con el servidor levantado y la cuenta `admin`:

1. Intente matricular a un estudiante en una sección de **otra carrera** → 409 con el mensaje de carrera.
2. Inscriba a un estudiante hasta llenar el cupo de una sección y busque una más → «está llena (n/n cupos)».
3. Cárguele tres notas a un matriculado: el promedio aparece solo (30/30/40) y, al faltar una, el estado
   es `EN_CURSO`.
4. Registre cuatro llamados con tres faltas: la matrícula pasa a `PERDIDO_POR_FALTA_DE_ASISTENCIA`
   aunque la nota sea 4.8.
5. Entre con la cuenta de estudiante y abra `/api/estudiantes` → debe ser 403, y `mi.html` debe mostrar
   solo sus datos.
6. Revise `/api/usuarios`: nunca debe traer la contraseña.
7. Intente borrar un curso que todavía tiene estudiantes matriculados → 409 pidiendo que primero se
   retiren las matrículas.

Esta lista está automatizada: `scripts/preparar-base-pruebas.sh` monta la base de pruebas
`universidad_e2e` (esquema + semilla + las cuentas `e2e_admin`, `e2e_prof` y `e2e_ana`) y
`scripts/verificar-api-e2e.sh` recorre las 202 comprobaciones contra el servidor apuntado a ella, con
prefijo `E2E` en todo lo que crea y borrándolo al final. **Ninguno de los dos toca `universidad`**. Son
guiones de `bash`: en Windows ejecútelos desde Git Bash (el de «Git for Windows») o desde la terminal WSL,
no desde el CMD. No son obligatorios para usar el sistema: son la verificación del equipo.

```bash
MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" \
  bash scripts/preparar-base-pruebas.sh su-usuario su-clave
DB_URL="jdbc:mysql://localhost:3306/universidad_e2e" DB_USER=su-usuario DB_PASSWORD=su-clave java -jar target/app.jar
bash scripts/verificar-api-e2e.sh        # ultima línea: TODO OK
```

## 10. Problemas frecuentes

| Síntoma | Causa y arreglo |
|---|---|
| `Port already in use: 8080` | Otra instancia del jar sigue viva. Cierre el proceso o cambie `PORT`. |
| Tildes rotos en el navegador | Servir siempre con `servirTexto(...)` y no tocar `<meta charset="UTF-8">` de las páginas. |
| `Access denied for user 'root'` | Contraseña equivocada en `application-local.properties` o en las variables de entorno. |
| `Table 'universidad.xxx' doesn't exist` | Falta el esquema (paso 2) o las migraciones (paso 2B). |
| `Unknown collation: 'utf8mb4_0900_ai_ci'` | MySQL 5.7 o MariaDB. El proyecto exige 8.0.16+ (vea el paso 0). |
| El login rechaza todas las cuentas / `usuarios` sale vacía | La semilla no crea cuentas: hay que generarlas con el paso 5, que es lo que calcula el hash. |
| `No plugin found for prefix 'exec'` con `-o` | PC recién montado: `~/.m2` está vacío. Ejecute el comando sin `-o` para que Maven baje el plugin. |
| `ClassNotFoundException: universidad.seguridad.CrearUsuario` | `exec:java` no compila. Use `mvn -q compile exec:java ...` (paso 5) o construya antes. |
| `Cannot access central ... in offline mode` | Primer build con `-o`. Baje las dependencias una vez sin `-o`; luego `-o` funciona. |
| `mvn` descarga de internet y falla | Use `-o` (offline) una vez que las dependencias ya están en `~/.m2`. |
| `instalar-pc.bat` dice `[FALTA] Maven en el PATH` | Su Maven no está en las carpetas de siempre. Los `Extension Pack for Java` de VS Code lo guardan dentro de `%USERPROFILE%\.m2\wrapper\...\bin\mvn.cmd` (el instalador lo busca ahí); si aun así no lo encuentra, dígasle la ruta: `set "MAVEN_BIN=C:\ruta\bin\mvn.cmd"` y vuelva a ejecutar el guion. |
| No puede borrar una sección | Regla: tiene matrículas. Retírelas primero (409 lo explica). |
| No puede borrar un curso | Regla: sus grupos aún tienen matrículas; la base lo borraría en cascada, así que el servicio lo corta con 409. |
| Un estudiante no puede inscribirse | Periodo no `ACTIVO`, curso de otra carrera, sin cupo o el tope de 20 créditos. |
