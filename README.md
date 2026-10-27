# UniversidadDao

Sistema de gestión académica con **dos interfaces sobre la misma base de datos MySQL**: una web (Javalin)
y una de escritorio (Swing). Todo funciona **en localhost**: no hay despliegue en la nube, ni datos en el
navegador, ni versiones sin servidor.

## Requisitos

- JDK 17 o superior (cualquiera: 17, 21 o 26; el `pom.xml` fija `--release 17`)
- Maven 3.8+ visible en la consola (`mvn -v`). Si solo lo trae su IDE, añada su carpeta `bin` al `PATH`,
  abra la terminal dentro del propio IDE, o use `instalar-pc.bat`, que también busca el Maven que
  `Extension Pack for Java` de VS Code deja en `%USERPROFILE%\.m2\wrapper`.
- MySQL **8.0.16 o superior** corriendo en `localhost:3306` con la base `universidad`. El esquema usa la
  colación `utf8mb4_0900_ai_ci` y restricciones `CHECK`: MySQL 5.7 y MariaDB rechazan ese archivo.
- Internet para el primer build: Maven baja unas 52 MB de dependencias. Después ya se puede trabajar sin
  red con `-o`.

¿Primera vez, o va a montar el proyecto un compañero? En Windows hay un atajo: **`instalar-pc.bat`** hace
los pasos de abajo (comprueba JDK/Maven/MySQL, crea la base, escribe sus credenciales, genera su cuenta de
acceso y compila). Si prefiere entender cada paso o trabaja en Linux/macOS, empiece por
[`docs/instalacion-paso-a-paso.md`](docs/instalacion-paso-a-paso.md), que trae los comandos copiables y
la tabla de problemas frecuentes. Las reglas académicas están en
[`docs/reglas-de-negocio.md`](docs/reglas-de-negocio.md) y el mapa del código en
[`docs/arquitectura.md`](docs/arquitectura.md).

## 1. Base de datos local

Cree el esquema desde cero (las 11 tablas, claves foráneas, `CHECK`, claves únicas y la columna
generada `promedio_final`). El archivo crea la base `universidad` si no existe:

```bash
mysql -u root -p < db/init/01-schema.sql
```

Para verlo funcionando sin cargar registros uno por uno, cargue los datos de demostración (5 facultades,
8 estudiantes, 13 secciones, casos de pérdida por asistencia):

```bash
mysql -u root -p universidad < db/seed/datos-demo.sql
```

Si ya tiene datos de un curso anterior, **no** ejecute el esquema (hace `DROP TABLE`): aplique en orden
`db/migrations/001_seguridad_y_fk.sql` y `db/migrations/002_reglas_academicas.sql`. El 002 se detiene a
propósito en el paso 3 para que asigne las matrículas que no encuentran sección; después compruebe el
resultado con `db/seed/comprobar-demo.sql`. `scripts/verificar-migracion.sh` ensaya todo ese recorrido
sobre una base de pruebas, sin tocar `universidad`.

### Credenciales

El clon **nunca trae contraseñas**: `application-local.properties` está en `.gitignore`, así que en su PC
no existe y hay que crearlo. Copie la plantilla (en Windows use `copy` en lugar de `cp`) y escriba el
usuario y la contraseña de su MySQL:

```bash
cp application-local.properties.example src/main/resources/application-local.properties
```

También puede usar variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`), que tienen prioridad.
**Nunca escriba contraseñas reales en el repositorio.**

### Primeras cuentas

La base nace sin usuarios y las contraseñas se guardan con hash PBKDF2. El cuarto argumento es la ficha
del rol: `id_estudiante` para un ESTUDIANTE, `id_profesor` para un PROFESOR, y se omite en ADMINISTRATIVO
(la cuenta no administrativa sin ficha no la crea ni la consola, porque MySQL no permite expresar esa
regla con un `CHECK` sobre columnas que tienen clave foránea). La contraseña mínima es de 10 caracteres
(`seguridad/CrearUsuario.java:28`). Estos comandos llevan `compile` delante y van **sin `-o`**: en un PC
nuevo todavía no existe `target/classes` y Maven necesita bajar el plugin `exec` de internet.

```bash
mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="admin UnaClaveDe10Caracteres ADMINISTRATIVO"

mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="profesor.cardozo OtraClaveLarga PROFESOR 1"    # id_profesor
mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario \
    -Dexec.args="estudiante.rojas OtraClaveLarga ESTUDIANTE 1"  # id_estudiante
```

Si tiene cuentas antiguas en texto plano, conviértalas con
`mvn -q compile exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords` (el login también migra
la cuenta que usa una contraseña sin hash).

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

Dos rutas más responden a preguntas que el formulario no debe adivinar: `GET /api/secciones/{id}/cupos`
(cuántos cupos quedan, calculado por el servidor; la responde ADMINISTRATIVO y el PROFESOR sobre sus
grupos) y `GET /api/estudiantes/{id}/secciones-inscribibles` (solo ADMINISTRATIVO: los grupos que ese
estudiante puede elegir, ya filtrados por carrera, periodo abierto, cupo disponible, créditos que le
caben y matrículas repetidas).

En el frontend basta con marcar cualquier enlace con `data-roles="ADMINISTRATIVO,PROFESOR"` para que
`aplicarPermisosDelMenu()` (`script.js`) lo oculte según el rol de la sesión; si el enlace está dentro de un
contenedor `.grupo`, el grupo entero se oculta cuando queda sin opciones. `menu.html` y la barra lateral de
`estudiantes.html` usan esa convención con las mismas rutas de la tabla. Ocultar un enlace es solo comodidad:
el servidor sigue validando cada petición.

### Lógica de negocio

Escala institucional de **0.0 a 5.0**; se aprueba con **3.0**. El promedio lo calcula MySQL con una columna
generada (30 % parcial 1 + 30 % parcial 2 + 40 % examen final) y el estado se deriva en el servidor:
`EN_CURSO` mientras falte una nota, `APROBADO` o `REPROBADO` al completarlas.

Encima de la nota manda la asistencia: con cuatro o más llamados, por debajo del **75 %** de asistencia la
materia se pierde con `PERDIDO_POR_FALTA_DE_ASISTENCIA` aunque el promedio acompañe. La inscripción suma
además sus propias condiciones: periodo `ACTIVO`, curso de la carrera del estudiante, cupo disponible, una
sola matrícula por sección y un tope de **20 créditos por periodo**.

Ninguna de esas decisiones la toma un formulario: viven en `universidad.servicio` (sin dependencias de
Javalin ni de Swing) y las ejecutan igual la API web y el cliente de escritorio. Al revés de la ruta,
borrar tampoco es libre: una sección con matriculados no se elimina y un curso que todavía tiene
matrículas tampoco, porque la base lo arrastraría en cascada junto con sus grupos. La lista completa,
con el mensaje que ve el usuario en cada caso, está en [`docs/reglas-de-negocio.md`](docs/reglas-de-negocio.md).

## 5. Pruebas

```bash
mvn clean test        # 77 pruebas: reglas académicas, validaciones, sesión, JSON y helpers HTTP
```

Las de `universidad.servicio` montan los DAO con dobles de Mockito, así que corren sin MySQL. Para
verificar la API contra una base real se usa un par de guiones, sobre `universidad_e2e` y **sin tocar
nunca `universidad`**:

```bash
MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" \
  bash scripts/preparar-base-pruebas.sh su-usuario su-clave    # esquema + semilla + tres cuentas de prueba
# en otra ventana, con el jar ya construido:
DB_URL="jdbc:mysql://localhost:3306/universidad_e2e" DB_USER=su-usuario DB_PASSWORD=su-clave java -jar target/app.jar
bash scripts/verificar-api-e2e.sh                  # 202 comprobaciones; sale 0 con TODO OK
```

El recorrido cubre las reglas de negocio (la sección decide curso y periodo, periodo abierto, carrera,
cupo, 20 créditos, 30/30/40, el 75 % de asistencia, la nota propagada a la matrícula y el borrado
protegido), la matriz de permisos por rol, las validaciones de forma, los filtros, las fechas ISO, la
salida de sesión y que `/api/usuarios` nunca devuelva la contraseña. Lo que crea lleva prefijo `E2E` y
se borra al final, en el orden que exigen las claves foráneas. El paso a paso del recorrido manual está
numerado en el paso 9 de [`docs/instalacion-paso-a-paso.md`](docs/instalacion-paso-a-paso.md), y el de
la migración en `scripts/verificar-migracion.sh`.

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
  App.java, LoginForm.java, MenuPrincipal.java      arranque y menú Swing
  EstudianteForm.java, CarreraForm.java             ventanas propias históricas
  ControladorWeb.java                               servidor Javalin, rutas de páginas y errores
  conexion/, dao/, modelo/, dto/                    pool, acceso a datos y entidades
  servicio/                                         reglas académicas compartidas por web y Swing
  seguridad/                                         PBKDF2, hash, límites de intentos, CrearUsuario
  swing/FormularioCRUD.java                         ventana CRUD genérica con los ganchos de escritura
  swing/ModulosSwing.java                           módulos del escritorio y sus permisos
  web/                                              controladores, sesión, validaciones, JSON
src/main/resources/public/
  logo-ut.png                                       logo oficial completo (se usa en login)
  logo-marca-ut.png                                  marca "U" recortada (cabeceras y favicon)
  marca.css                                         tonos institucionales, botones y acento por módulo
  menu.html, menu.css                               panel de control con las tarjetas de módulos
  modulos.css, modulos.js, script.js                páginas CRUD y su motor genérico
  estudiantes.html, carreras.html                   las dos pantallas con maquetado propio
db/init/01-schema.sql                               esquema completo, de cero
db/migrations/001_seguridad_y_fk.sql                claves foráneas y permisos sobre una base existente
db/migrations/002_reglas_academicas.sql             sección en la matrícula y restricciones de las reglas
db/seed/datos-demo.sql                              datos de demostración
db/seed/comprobar-demo.sql                          las reglas repetidas en SQL, para validar la base
tablas-MySql/                                       volcado de una tabla por archivo (la base original)
scripts/preparar-base-pruebas.sh                     base de pruebas universidad_e2e + sus tres cuentas
scripts/verificar-api-e2e.sh                         202 comprobaciones de la API contra esa base
scripts/verificar-migracion.sh                       ensayo completo de 001 + 002 sobre una base de pruebas
docs/                                               instalación, reglas de negocio y arquitectura
instalar-pc.bat                                     monta el proyecto en un PC nuevo (pasos 1 a 6)
run-app.bat                                         arranca la interfaz web con las credenciales locales
CHANGELOG.md                                        qué cambió y en qué fecha
```

## 8. Identidad visual

`marca.css` concentra los tonos institucionales (verde `#0b6e4f`, oro `#f0b429` y neutros), los botones y el
**acento único de cada módulo**: el panel (`menu.html`) lo aplica a la barra lateral, el icono y la sombra de
su tarjeta, y cada página CRUD lo hereda con `body data-modulo="..."` (título, botones, cabeceras de tabla y
filas al pasar el cursor). Los iconos son SVG en línea, sin dependencias externas ni CDN: el proyecto solo
funciona en localhost.

El logo oficial está en `src/main/resources/public/` con el fondo ya vaciado a transparencia:
`logo-ut.png` (logo completo con leyenda y sello, centrado en `login.html`) y `logo-marca-ut.png` (solo la
marca "U" en cuadro, para las cabeceras de `menu.html` y de las páginas CRUD y para el favicon). Las dos se
sirven desde `ControladorWeb.java` con `servirBinario(ctx, "image/png", recurso)`; ese helper usa
`Recursos.leerBytes`, porque `Recursos.leer` decodifica con UTF-8 y destruiría los bytes del PNG. Para
cambiar el archivo basta con sobrescribir el PNG con el mismo nombre.

**Codificación:** todos los `.html`, `.css` y `.js` se guardan en UTF-8, cada documento declara
`<meta charset="UTF-8">`, cada hoja de estilos declara `@charset "UTF-8";` y el servidor responde siempre
`charset=utf-8`. Si agrega una página o un recurso nuevo, sirva el texto con
`servirTexto(ctx, tipo, recurso)` (`ControladorWeb`): `ctx.result(String)` codifica con el charset de la
respuesta y, sin esa declaración, Jetty usa ISO-8859-1 y rompe tildes y eñes.
