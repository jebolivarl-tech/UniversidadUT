# UniversidadDao

Sistema de gestión universitaria con aplicación de escritorio Swing, interfaz web con Javalin y una versión web autónoma para uso local.

## Requisitos

- JDK 17 o superior
- Maven
- MySQL 8.x con la base de datos `universidad` y las tablas requeridas por los DAO

## Configurar la base de datos

En Windows CMD, configura las variables antes de iniciar la aplicación:

```bat
set DB_URL=jdbc:mysql://localhost:3306/universidad
set DB_USER=tu_usuario
set DB_PASSWORD=tu_contrasena
```

`DB_USER` y `DB_PASSWORD` son **obligatorias**: como variables de entorno o, solo en desarrollo local, en `src/main/resources/application-local.properties` (ya incluido; edite la contraseña de su MySQL). `DB_URL` por defecto es `jdbc:mysql://localhost:3306/universidad`. Ese archivo está en `.gitignore` y `.dockerignore`. Esquema inicial: `db/init/01-schema.sql`; despliegue: ver `DEPLOY.md`.

## Ejecutar

### Interfaz web local sin servidor

Abre directamente `src/main/resources/public/universidad-local.html` en el navegador. En el primer uso podrás crear una cuenta local; estudiantes y carreras se guardan en el almacenamiento de ese navegador. No requiere Maven, MySQL ni conexión a internet. Los datos no se comparten con la aplicación Swing ni con la base de datos, y esta modalidad no es adecuada para información sensible.

### Aplicación web conectada a MySQL

Iniciar el servidor web y su API en `http://localhost:8080`:

```bash
mvn exec:java
```

Luego abre `http://localhost:8080/login.html`. Para iniciar la versión Swing:

```bash
mvn exec:java -Dexec.mainClass=universidad.App
```

El diagnóstico de conexión está disponible en `http://localhost:8080/api/health`. El login requiere una fila en `universidad.usuarios` con `estado = 1`; las contraseñas se guardan con hash PBKDF2 (las antiguas en texto plano se migran al iniciar sesión, o con `universidad.seguridad.MigrarPasswords`). Cree usuarios con `universidad.seguridad.CrearUsuario`. Permisos: `ADMINISTRATIVO` gestiona todo, `PROFESOR` consulta estudiantes y carreras, `ESTUDIANTE` solo consulta carreras; se validan en el servidor.

Validar compilación y pruebas:

```bash
mvn clean test
```
