@echo off
cd /d "%~dp0"

REM Configure estas variables ANTES de ejecutar (no guarde contrasenas reales en este archivo):
REM   set DB_USER=su_usuario
REM   set DB_PASSWORD=su_contrasena
REM   set DB_URL=jdbc:mysql://localhost:3306/universidad   (opcional)
if "%DB_USER%"=="" (
  echo Falta la variable DB_USER. Ejecute: set DB_USER=su_usuario
  exit /b 1
)
if "%DB_PASSWORD%"=="" (
  echo Falta la variable DB_PASSWORD. Ejecute: set DB_PASSWORD=su_contrasena
  exit /b 1
)

echo =====================================================
echo Iniciando UniversidadDao en http://localhost:8080
echo =====================================================
mvn exec:java
