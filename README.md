# UniversidadDao

Sistema de gestión académica con **dos interfaces sobre la misma base de datos MySQL**: una web (Javalin)
y una de escritorio (Swing). Todo funciona **en localhost**: no hay despliegue en la nube, ni datos en el
navegador, ni versiones sin servidor.

## Requisitos

- JDK 17 o superior
- Maven 3.8+
- MySQL 8.x corriendo en `localhost:3306` con la base `universidad`

## 1. Base de datos local

Cree la base y el esquema (las 11 tablas, claves foráneas y la columna generada `promedio_final`):

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS universidad"
mysql -u root -p universidad < db/init/01-schema.sql
```

Si ya tiene datos, no ejecute el esquema: aplique solo `db/001_seguridad_y_fk.sql`.

### Credenciales

Copie la plantilla y edite la contraseña de su MySQL. El archivo queda fuera de git:

```bash
cp application-local.properties.example src/main/resources/application-local.properties
```

También puede usar variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`), que tienen prioridad.
**Nunca escriba contraseñas reales en el repositorio.**

### Primer administrador

La base nace sin usuarios y las contraseñas se guardan con hash PBKDF2:

```bash
mvn exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="admin UnaClaveDe10Caracteres ADMINISTRATIVO"
```

Si tiene cuentas antiguas en texto plano, conviértalas con
`mvn exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords` (el login también migra la cuenta
que usa una contraseña sin hash).

## 2. Interfaz web

```bash
mvn clean package
java -jar target/app.jar
```

Abra <http://localhost:8080/login.html>. `run-app.bat` hace lo mismo en Windows leyendo las variables de
entorno. Sin empaquetar, puede lanzarla con `mvn exec:java`.

Diagnóstico: <http://localhost:8080/api/health> responde `{"status":"UP","database":"UP"}`.

Opcional, app + MySQL en contenedores dentro del mismo equipo:

```bash
cp .env.example .env   # edite las contraseñas
docker compose up -d --build
```

## 3. Cliente de escritorio (Swing)

```bash
mvn exec:java -Dexec.mainClass=universidad.App
```

o, con el jar ya construido, `java -cp target/app.jar universidad.App`. Inicia sesión con la misma cuenta
que la web y abre el panel de control con los módulos permitidos para su rol. Las ventanas hablan directo
con MySQL por los DAO, sin pasar por la API.

## 4. Módulos y permisos

La autorización se aplica **siempre en el servidor** (API web) y se refleja en cada interfaz.

| Módulo | Página web | API | ADMINISTRATIVO | PROFESOR | ESTUDIANTE |
|---|---|---|---|---|---|
| Mi información | `mi.html` | `/api/mi*` | lectura | lectura | lectura (sus datos) |
| Estudiantes | `estudiantes.html` | `/api/estudiantes` | CRUD | lectura | sin acceso |
| Carreras | `carreras.html` | `/api/carreras` | CRUD | lectura | sin acceso |
| Facultades | `facultades.html` | `/api/facultades` | CRUD | lectura | sin acceso |
| Profesores | `profesores.html` | `/api/profesores` | CRUD | lectura | sin acceso |
| Periodos académicos | `periodos.html` | `/api/periodos` | CRUD | lectura | sin acceso |
| Cursos | `cursos.html` | `/api/cursos` | CRUD | lectura | sin acceso |
| Secciones | `secciones.html` | `/api/secciones` | CRUD | lectura (solo las suyas) | sin acceso |
| Matrículas | `matriculas.html` | `/api/matriculas` | CRUD | lectura (cursos de sus secciones) | sin acceso |
| Asistencias | `asistencias.html` | `/api/asistencias` | CRUD | escribe en sus secciones | sin acceso |
| Calificaciones | `calificaciones.html` | `/api/calificaciones` | CRUD | escribe en sus secciones | sin acceso |
| Cuentas de acceso | `usuarios.html` | `/api/usuarios` | CRUD | sin acceso | sin acceso |

En Swing el panel muestra exactamente los mismos módulos: el administrativo los edita todos; el profesor
consulta los catálogos y escribe asistencias y calificaciones de sus secciones; el estudiante abre solo
`Mis secciones`, `Mis matrículas`, `Mis asistencias` y `Mis calificaciones` en modo lectura.

Las páginas y endpoints no autorizados devuelven 403 (o redirigen a `login.html` si no hay sesión).

### Calificaciones

Escala institucional de **0.0 a 5.0**; se aprueba con **3.0**. El promedio lo calcula MySQL con una columna
generada (30 % parcial 1 + 30 % parcial 2 + 40 % examen final) y el estado se deriva en el servidor:
`EN_CURSO` mientras falte una nota, `APROBADO` o `REPROBADO` al completarlas.

## 5. Pruebas

```bash
mvn clean test        # 32 pruebas unitarias (validaciones, notas, helpers HTTP)
```

Para verificar la API contra su MySQL local, inicie el servidor y compruebe al menos: login correcto e
incorrecto, un `POST` y un `PUT` por módulo, y que un `ESTUDIANTE` reciba 403 en `/api/estudiantes`.
`/api/usuarios` nunca debe incluir la contraseña.

## 6. Seguridad

- Contraseñas con PBKDF2-HMAC-SHA256 (600 000 iteraciones) y comparación en tiempo constante.
- La contraseña solo se recibe por JSON (`@JsonProperty(WRITE_ONLY)`); ninguna respuesta la devuelve.
- Sesión en servidor con cookie `HttpOnly` y `SameSite=Lax`, invalidación al iniciar sesión (anti
  fijación de sesión) y expiración por inactividad de 30 minutos.
- Intentos de acceso limitados por IP y usuario (`LoginRateLimiter`); CORS restringido a una lista de
  orígenes (localhost por defecto); cabeceras `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`
  y `Referrer-Policy: no-referrer`.
- El front-end escribe siempre con `textContent` (sin `innerHTML`), y las validaciones de tamaño, escala
  de notas y fechas se repiten en el servidor (`Validaciones`).

## 7. Estructura

```
src/main/java/universidad/
  App.java, LoginForm.java, MenuPrincipal.java     arranque y menú Swing
  EstudianteForm.java, CarreraForm.java            ventanas propias históricas
  ControladorWeb.java                              servidor Javalin y rutas de páginas
  conexion/, dao/, modelo/, dto/                   acceso a datos y entidades
  seguridad/                                       PBKDF2, hash, límites de intentos
  swing/FormularioCRUD.java                        ventana CRUD genérica
  swing/ModulosSwing.java                          módulos del escritorio y sus permisos
  web/                                             controladores, sesión, validaciones, JSON
src/main/resources/public/                         HTML, CSS y JS de la interfaz web
db/init/01-schema.sql                              esquema completo
tablas-MySql/                                      volcado de una tabla por archivo
```
