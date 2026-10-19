@echo off
setlocal
cd /d "%~dp0"

REM Arranca la interfaz web en http://localhost:8080 usando el MySQL de este equipo.
REM Credenciales: src/main/resources/application-local.properties (ver application-local.properties.example)
REM o variables de entorno DB_USER y DB_PASSWORD, que tienen prioridad.

if exist "src\main\resources\application-local.properties" goto ejecutar
if exist "application-local.properties" goto ejecutar
if defined DB_USER if defined DB_PASSWORD goto ejecutar

echo No se encontro la configuracion de la base de datos.
echo Cree src\main\resources\application-local.properties a partir de
echo application-local.properties.example, o defina las variables de entorno:
echo     set DB_USER=su_usuario
echo     set DB_PASSWORD=su_contrasena
exit /b 1

:ejecutar
if not exist "target\app.jar" (
  echo Construyendo target\app.jar ...
  call mvn -B clean package -DskipTests || exit /b 1
)

echo =====================================================
echo Iniciando UniversidadDao en http://localhost:8080
echo =====================================================
java -jar target\app.jar
endlocal
