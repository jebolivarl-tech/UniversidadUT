@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

REM Arranca la interfaz web en http://localhost:8080 usando el MySQL de este equipo.
REM Credenciales: src/main/resources/application-local.properties (ver application-local.properties.example)
REM o variables de entorno DB_USER y DB_PASSWORD, que tienen prioridad.

if exist "src\main\resources\application-local.properties" goto ejecutar
if exist "application-local.properties" goto ejecutar
if defined DB_USER if defined DB_PASSWORD goto ejecutar

echo No se encontró la configuración de la base de datos.
echo En un PC nuevo, ejecute instalar-pc.bat: crea la base, escribe esta configuración,
echo genera su cuenta y compila el proyecto.
echo Si prefiere hacerlo a mano, cree src\main\resources\application-local.properties a partir de
echo application-local.properties.example, o defina las variables de entorno:
echo     set DB_USER=su_usuario
echo     set DB_PASSWORD=su_contrasena
exit /b 1

:ejecutar
if exist "target\app.jar" goto arrancar
where mvn >nul 2>nul
if errorlevel 1 (
  echo Falta target\app.jar y Maven no esta en el PATH.
  echo Ejecute instalar-pc.bat: busca Maven donde lo tenga su IDE y compila.
  exit /b 1
)
echo Construyendo target\app.jar ...
call mvn -B clean package -DskipTests || exit /b 1

:arrancar

echo =====================================================
echo Iniciando UniversidadDao en http://localhost:8080
echo =====================================================
java -jar target\app.jar
endlocal
