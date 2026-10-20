# Instalación paso a paso (solo localhost)

Guía para montar el proyecto en un equipo desde cero. Está pensada para Windows, pero los comandos de
Maven y MySQL son los mismos en Linux o macOS.

## 0. Qué necesita tener instalado

| Herramienta | Versión mínima | Para qué |
|---|---|---|
| JDK | 17 | Compilar y ejecutar (el `pom.xml` fija `--release 17`). |
| Maven | 3.8 | Construir el `target/app.jar`. |
| MySQL | 8.0 | La base `universidad`, corriendo en `localhost:3306`. |

Verifique antes de empezar:

```bash
java -version    # openjdk 17 o superior
mvn -v
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p -e "SELECT VERSION();"
```

## 1. Conseguir el código

```bash
git clone https://github.com/jebolivarl-tech/UniversidadUT.git UniversidadDao
cd UniversidadDao
```

Si prefiere el ZIP, descomprímalo y entre en la carpeta `UniversidadDao-corregido`.

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

## 3. Datos de demostración (opcional)

Para ver el sistema funcionando sin cargar registros uno por uno:

```bash
mysql -u root -p universidad < db/seed/datos-demo.sql
```

Incluye 5 facultades, 7 carreras, 8 estudiantes, 13 secciones, matrículas con notas y casos reales de
pérdida por asistencia. **No lo ejecute sobre una base con datos.** Las cuentas de acceso se crean en
el paso 5.

## 4. Credenciales de la base

Copie la plantilla y escriba su contraseña local de MySQL. El archivo está en `.gitignore`:

```bash
cp application-local.properties.example src/main/resources/application-local.properties
```

```properties
DB_URL=jdbc:mysql://localhost:3306/universidad
DB_USER=root
DB_PASSWORD=la_suya
```

Las variables de entorno `DB_USER` y `DB_PASSWORD` tienen prioridad sobre el archivo, y `DB_POOL_SIZE`
ajusta el tamaño del pool. **Nunca suba contraseñas al repositorio.**

## 5. Primeras cuentas de acceso

La base nace sin usuarios y las contraseñas se guardan con hash PBKDF2. El cuarto argumento es la ficha
del rol: `id_estudiante` para ESTUDIANTE, `id_profesor` para PROFESOR, y se omite en ADMINISTRATIVO.

```bash
mvn -q -o exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="admin UnaClaveLarga123 ADMINISTRATIVO"

mvn -q -o exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="profesor.cardozo OtraClaveLarga123 PROFESOR 1"

mvn -q -o exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="estudiante.rojas ClaveDeAna2026 ESTUDIANTE 1"
```

Si viene de una base antigua con contraseñas en texto plano:
`mvn -q -o exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords` (el primer login también
convierte la cuenta usada).

## 6. Compilar y probar

```bash
mvn -o clean test       # 77 pruebas: reglas académicas, validaciones, sesión, JSON
mvn -o clean package    # genera target/app.jar (levante el servidor antes si está ocupado)
```

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
prefijo `E2E` en todo lo que crea y borrándolo al final. **Ninguno de los dos toca `universidad`**:

```bash
bash scripts/preparar-base-pruebas.sh root su-clave
DB_URL="jdbc:mysql://localhost:3306/universidad_e2e" DB_USER=root DB_PASSWORD=su-clave java -jar target/app.jar
bash scripts/verificar-api-e2e.sh        # ultima línea: TODO OK
```

## 10. Problemas frecuentes

| Síntoma | Causa y arreglo |
|---|---|
| `Port already in use: 8080` | Otra instancia del jar sigue viva. Cierre el proceso o cambie `PORT`. |
| Tildes rotos en el navegador | Servir siempre con `servirTexto(...)` y no tocar `<meta charset="UTF-8">` de las páginas. |
| `Access denied for user 'root'` | Contraseña equivocada en `application-local.properties` o en las variables de entorno. |
| `Table 'universidad.xxx' doesn't exist` | Falta el esquema (paso 2) o las migraciones (paso 2B). |
| `mvn` descarga de internet y falla | Use `-o` (offline) una vez que las dependencias ya están en `~/.m2`. |
| No puede borrar una sección | Regla: tiene matrículas. Retírelas primero (409 lo explica). |
| No puede borrar un curso | Regla: sus grupos aún tienen matrículas; la base lo borraría en cascada, así que el servicio lo corta con 409. |
| Un estudiante no puede inscribirse | Periodo no `ACTIVO`, curso de otra carrera, sin cupo o el tope de 20 créditos. |
