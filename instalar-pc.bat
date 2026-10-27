@echo off
REM Instalador de UniversidadDao para un PC nuevo. Solo localhost, sin nube.
REM Hace los pasos 2 a 6 de docs/instalacion-paso-a-paso.md: comprueba las herramientas, crea la base,
REM escribe las credenciales locales, genera su cuenta administrativa y compila. Es reutilizable: si algo
REM fallo, vuelva a ejecutarlo y retoma desde donde se detuvo.
REM Gobernable sin preguntas con variables de entorno:
REM   DB_NOMBRE  DB_USER  DB_PASSWORD  CARGAR_DEMO=S/N  CREAR_CUENTA=S/N  ADMIN_USUARIO  ADMIN_CLAVE
REM   Y para las herramientas fuera del PATH:  MYSQL_BIN   MAVEN_BIN   SOBREESCRIBIR=S/N
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"
if "%DB_NOMBRE%"=="" set "DB_NOMBRE=universidad"
set "OUT=%TEMP%\ut-instalador.txt"

echo ============================================================
echo  UniversidadDao - instalacion en este PC
echo ============================================================
echo.

call :comprobar_herramientas || exit /b 1
call :leer_credenciales      || exit /b 1
call :comprobar_mysql        || exit /b 1
call :preparar_base          || exit /b 1
call :escribir_configuracion || exit /b 1
call :crear_cuenta           || exit /b 1
call :compilar               || exit /b 1

echo.
echo ============================================================
echo  Listo. Para arrancar:  run-app.bat   (o  java -jar target\app.jar)
if not defined PORT set "PORT=8080"
echo  Abra  http://localhost:%PORT%/login.html
if defined ADMIN_USUARIO echo  Entre con el usuario  !ADMIN_USUARIO!
echo.
echo  Alta de un profesor o un estudiante, cuando la necesite:
echo    "%MVN%" compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario ^
echo      -Dexec.args="nombre.apellido SuClave12345 PROFESOR 1"
echo    (el ultimo numero es id_profesor o id_estudiante; ADMINISTRATIVO va sin cuarto dato)
echo ============================================================
del "%OUT%" >nul 2>nul
endlocal
exit /b 0

REM ================================================================= herramientas
:buscar_maven
REM Deja en MVN la forma de invocar Maven: la linea de comandos si esta en el PATH, o el mvn.cmd de
REM alguna de las carpetas donde lo dejan los instaladores y los IDEs mas comunes.
set "MVN="
if defined MAVEN_BIN (
  if exist "!MAVEN_BIN!" (
    set "MVN=!MAVEN_BIN:/=\!"
    exit /b 0
  )
)
where mvn >nul 2>nul
if not errorlevel 1 (
  set "MVN=mvn"
  exit /b 0
)
if defined M2_HOME (
  if exist "!M2_HOME!\bin\mvn.cmd" (
    set "MVN=!M2_HOME!\bin\mvn.cmd"
    exit /b 0
  )
)
for %%d in ("%USERPROFILE%\tools\apache-maven-3.9.16\bin\mvn.cmd" "%LOCALAPPDATA%\Programs\apache-maven\bin\mvn.cmd" "C:\Program Files\apache-maven-3.9.16\bin\mvn.cmd" "C:\Program Files\apache-maven-3.9.9\bin\mvn.cmd" "C:\maven\bin\mvn.cmd" "%LOCALAPPDATA%\Code\User\globalStorage\pleiades.java-extension-pack-jdk\maven\latest\bin\mvn.cmd") do (
  if exist %%d set "MVN=%%~d"
)
exit /b 0

:comprobar_herramientas
where java >nul 2>nul
if errorlevel 1 (
  echo [FALTA] El JDK. Instale Java 17 o superior y agreguelo al PATH ^(prueba: java -version^).
  exit /b 1
)
java -version >"%OUT%" 2>&1
set /p LINEA1=<"%OUT%"
echo [ok] !LINEA1!

call :buscar_maven
if not defined MVN (
  echo [FALTA] Maven en el PATH. Instale Maven 3.8+ y agregue su carpeta bin al PATH ^(prueba: mvn -v^),
  echo         o indique donde lo tiene:
  echo           set "MAVEN_BIN=C:\Program Files\apache-maven-3.9.16\bin\mvn.cmd"
  echo           instalar-pc.bat
  echo         Si solo lo trae su IDE, tambien sirve la terminal de ese IDE.
  exit /b 1
)
call "%MVN%" -v >"%OUT%" 2>&1
set "LINEA1="
set /p LINEA1=<"%OUT%"
if not defined LINEA1 (
  echo [ERROR] Maven esta en "!MVN!" pero no responde. Pruebelo a mano para ver el fallo:
  echo           "%MVN%" -v
  exit /b 1
)
echo [ok] !LINEA1!

if defined MYSQL_BIN (
  echo [ok] mysql indicado en MYSQL_BIN: %MYSQL_BIN%
  exit /b 0
)
where mysql >nul 2>nul
if not errorlevel 1 (
  set "MYSQL_BIN=mysql"
  echo [ok] mysql en el PATH
  exit /b 0
)
for %%d in ("C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.2\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe") do (
  if exist %%d set "MYSQL_BIN=%%~d"
)
if defined MYSQL_BIN (
  echo [ok] %MYSQL_BIN%
  exit /b 0
)
echo [FALTA] El cliente mysql.exe. Instale MySQL Server 8.0.16 o superior, o indique la ruta:
echo           set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
echo           instalar-pc.bat
exit /b 1

REM ================================================================= credenciales locales
:leer_credenciales
if "%DB_USER%"=="" set /p "DB_USER=Usuario de su MySQL local [root]: "
if "%DB_USER%"=="" set "DB_USER=root"
if "%DB_PASSWORD%"=="" set /p "DB_PASSWORD=Contraseña de %DB_USER% (se ve mientras escribe; es un desarrollo local): "
if "%DB_PASSWORD%"=="" (
  echo [ERROR] Sin contraseña no se puede continuar.
  exit /b 1
)
set "MYSQL_PWD=%DB_PASSWORD%"
exit /b 0

REM ================================================================= conexion y version
:comprobar_mysql
set "VER="
"!MYSQL_BIN!" -h 127.0.0.1 -P 3306 -u "%DB_USER%" -N -e "SELECT VERSION()" >"%OUT%" 2>"%OUT%.err"
set /p VER=<"%OUT%"
if not defined VER (
  echo [ERROR] MySQL no responde en localhost:3306 como %DB_USER%.
  set /p DETALLE=<"%OUT%.err"
  echo         !DETALLE!
  echo         Compruebe que el servicio MySQL este iniciado y vuelva a ejecutar este guion.
  exit /b 1
)
echo [ok] conectado a MySQL !VER!
REM La colacion utf8mb4_0900_ai_ci del esquema no existe en MySQL 5.7 ni en MariaDB.
"!MYSQL_BIN!" -h 127.0.0.1 -P 3306 -u "%DB_USER%" -N -e "CREATE DATABASE _ut_probe CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; DROP DATABASE _ut_probe;" >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Su MySQL rechaza la colacion utf8mb4_0900_ai_ci que usa db/init/01-schema.sql.
  echo         Necesita MySQL 8.0.16 o superior: los CHECK tampoco se aplican antes de esa version.
  exit /b 1
)
echo [ok] la version admite el esquema del proyecto
exit /b 0

REM ================================================================= base de datos
:preparar_base
set "TABLAS="
"!MYSQL_BIN!" -h 127.0.0.1 -P 3306 -u "%DB_USER%" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='%DB_NOMBRE%'" >"%OUT%" 2>nul
set /p TABLAS=<"%OUT%"
if not defined TABLAS set "TABLAS=0"
if "%TABLAS%"=="0" goto aplicar_esquema

echo [!!] La base "%DB_NOMBRE%" ya existe con %TABLAS% tablas.
echo      db/init/01-schema.sql hace DROP de las once: si lo aplica, pierde lo que hay ahi.
set "RESP=N"
if not defined SOBREESCRIBIR set /p "RESP=¿Borrarla y dejarla de cero? S/NO [NO]: "
if defined SOBREESCRIBIR set "RESP=%SOBREESCRIBIR%"
if /i not "!RESP!"=="S" if /i not "!RESP!"=="SI" (
  echo [--] Se queda la base tal como esta. Si le falta la tabla secciones o la columna id_seccion,
  echo      aplique db/migrations/001_seguridad_y_fk.sql y db/migrations/002_reglas_academicas.sql
  echo      y vuelva a ejecutar este guion.
  goto preguntar_demo
)
"!MYSQL_BIN!" -h 127.0.0.1 -P 3306 -u "%DB_USER%" -e "DROP DATABASE \`%DB_NOMBRE%\`" >nul 2>nul

:aplicar_esquema
echo [--] Aplicando db/init/01-schema.sql a la base "%DB_NOMBRE%"
call :copiar_sql "db\init\01-schema.sql" "%TEMP%\ut-schema.sql"
"!MYSQL_BIN!" -h 127.0.0.1 -P 3306 -u "%DB_USER%" < "%TEMP%\ut-schema.sql"
if errorlevel 1 (
  echo [ERROR] El esquema no se pudo aplicar.
  del "%TEMP%\ut-schema.sql" >nul 2>nul
  exit /b 1
)
del "%TEMP%\ut-schema.sql" >nul 2>nul
echo [ok] esquema creado

:preguntar_demo
if "%CARGAR_DEMO%"=="" set /p "CARGAR_DEMO=¿Cargar los datos de demostracion? S/NO  ^(8 estudiantes, 13 secciones, 14 matriculas^): "
if /i not "!CARGAR_DEMO!"=="S" if /i not "!CARGAR_DEMO!"=="SI" (
  echo [--] Sin datos de demostracion: el sistema arranca vacio y se alimenta desde la interfaz.
  goto base_ok
)
echo [--] Aplicando db/seed/datos-demo.sql
call :copiar_sql "db\seed\datos-demo.sql" "%TEMP%\ut-demo.sql"
"!MYSQL_BIN!" -h 127.0.0.1 -P 3306 -u "%DB_USER%" < "%TEMP%\ut-demo.sql"
if errorlevel 1 (
  echo [ERROR] La semilla no cargo. Suele ser porque esa base ya tiene esas filas.
  del "%TEMP%\ut-demo.sql" >nul 2>nul
  exit /b 1
)
del "%TEMP%\ut-demo.sql" >nul 2>nul
echo [ok] datos de demostracion cargados

:base_ok
exit /b 0

REM ================================================================= archivo de credenciales
:escribir_configuracion
if not exist "src\main\resources" mkdir "src\main\resources"
> "src\main\resources\application-local.properties" echo DB_URL=jdbc:mysql://localhost:3306/%DB_NOMBRE%
>> "src\main\resources\application-local.properties" echo DB_USER=%DB_USER%
>> "src\main\resources\application-local.properties" echo DB_PASSWORD=%DB_PASSWORD%
echo [ok] src\main\resources\application-local.properties creado. Esta en .gitignore: no sube a git
echo      ni va en el ZIP que comparta con sus companeros.
exit /b 0

REM ================================================================= cuenta de acceso
:crear_cuenta
if "%CREAR_CUENTA%"=="" set /p "CREAR_CUENTA=¿Crear una cuenta ADMINISTRATIVO para usted? S/NO: "
if /i not "!CREAR_CUENTA!"=="S" if /i not "!CREAR_CUENTA!"=="SI" (
  echo [!!] Sin cuenta ADMINISTRATIVO no habra con quien iniciar sesion. Puede crearla cuando quiera:
  echo        mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario ^
  echo          -Dexec.args="admin SuClave12345 ADMINISTRATIVO"
  exit /b 0
)
if "%ADMIN_USUARIO%"=="" set /p "ADMIN_USUARIO=Usuario, sin espacios: "
if "%ADMIN_CLAVE%"=="" set /p "ADMIN_CLAVE=Clave de 10 caracteres o mas: "
if "!ADMIN_CLAVE:~9,1!"=="" (
  echo [ERROR] La clave tiene menos de 10 caracteres y la aplicacion la rechaza
  echo         ^(seguridad/CrearUsuario.java:28^).
  exit /b 1
)
echo [--] Compilando y creando la cuenta !ADMIN_USUARIO!
call "%MVN%" -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario -Dexec.args="!ADMIN_USUARIO! !ADMIN_CLAVE! ADMINISTRATIVO"
if errorlevel 1 (
  echo [ERROR] No se pudo crear la cuenta. Si habla de Access denied, revise la contraseña del paso
  echo         de credenciales y vuelva a ejecutar este guion.
  exit /b 1
)
echo [ok] cuenta ADMINISTRATIVO "!ADMIN_USUARIO!" creada
exit /b 0

REM ================================================================= compilar
:compilar
echo [--] mvn clean package: compila y corre las 77 pruebas. En el primer build de este PC baja
echo      unas 52 MB de dependencias, asi que necesita internet.
call "%MVN%" clean package
if errorlevel 1 (
  echo [ERROR] Fallo la compilacion o una prueba. Si tiene el servidor levantado, cierrelo: la JVM
  echo         mantiene bloqueado target\app.jar y mvn clean no puede borrarlo.
  exit /b 1
)
echo [ok] target\app.jar generado
exit /b 0

REM ================================================================= utilidades
:copiar_sql
REM Copia el archivo SQL a la ruta pedida, cambiando el nombre de la base si no es "universidad".
set "SRC_SQL=%~1"
set "DST_SQL=%~2"
if /i "%DB_NOMBRE%"=="universidad" (
  copy /y "%SRC_SQL%" "%DST_SQL%" >nul
  exit /b 0
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Content -Raw -LiteralPath '%SRC_SQL%' -Encoding UTF8) -replace 'universidad','%DB_NOMBRE%' | Set-Content -NoNewline -LiteralPath '%DST_SQL%' -Encoding UTF8"
exit /b 0
