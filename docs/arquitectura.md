# Arquitectura

Un solo modelo de datos y un solo conjunto de reglas, servidos por dos interfaces distintas sobre la
misma máquina. No hay servicios remotos, ni colas, ni despliegue: todo ocurre contra MySQL en
`localhost`.

```
 navegador ── HTTP/JSON ──┐
                          ├──> ControladorWeb (Javalin) ──> ModulosCrud ──> servicios ──> DAO ──> MySQL
 Swing (App) ── JDBC ─────┘                                          └────> DAO ──────┘
```

## Paquetes

| Paquete | Responsabilidad |
|---|---|
| `universidad.conexion` | `Conexion`: pool HikariCP, URL y credenciales desde `application-local.properties` o variables de entorno. |
| `universidad.modelo` | Entidades planas + Jackson. Los campos derivados se marcan `@JsonProperty(access = READ_ONLY)` para que el cliente no pueda escribirlos. |
| `universidad.dao` | Una interfaz por entidad (`CRUD<T>`) y su implementación JDBC. Es la costura que permite probar los servicios sin base de datos. |
| `universidad.servicio` | **Regla de negocio.** Sin dependencias de Javalin ni de Swing: la usan igual la API y el escritorio. |
| `universidad.web` | `ControladorWeb`/`ModulosCrud` (rutas y permisos), `Sesion` (roles), `Validaciones` (formato), `Http` (JSON y errores de JDBC), `MiController` (panel personal). |
| `universidad.seguridad` | PBKDF2 (`PasswordHasher`), `MigrarPasswords`, `LoginRateLimiter` y `CrearUsuario` (alta por consola). |
| `universidad.swing` | `FormularioCRUD` (ventana genérica con los mismos ganchos de escritura que la API) y `ModulosSwing` (módulos, permisos y servicios del escritorio). |
| `resources/public` | Frontend declarativo: cada página es un fichero de 54 líneas con `data-modulo` y el motor está en `modulos.js`. |

## Camino de una escritura

`POST /api/matriculas` (y su equivalente en Swing) siempre sigue el mismo orden:

1. `Sesion.requerirRol` — el rol se comprueba en el servidor, nunca en el menú.
2. `Validaciones.esMatriculaValida` — formato: ids positivos, nota en la escala.
3. `ServicioMatriculas.preparar` — reglas académicas; deriva curso, periodo y estado.
4. DAO `registrar`/`actualizar` — JDBC puro.
5. `despuesEscribir` — efectos sobre otros registros (la definitiva propaga la nota a la matrícula).

En el borrado se interpone `antesEliminar` (una sección con matriculados no se borra). `FormularioCRUD`
expone los mismos tres ganchos —`antesEscribir`, `despuesEscribir`, `antesEliminar`— para que el
escritorio no mantenga una segunda versión de las reglas.

## Errores

- `ReglaVioladaException` lleva el código HTTP en la propia excepción: **409** si la acción choca con
  el estado académico (periodo cerrado, sección llena, tope de créditos) y **400** si una referencia no
  existe. `ControladorWeb` la traduce a `{"error": "…"}`.
- `ApiException` cubre 401/403/404/500 de las rutas.
- `CrudController.traducir` convierte `SQLException` en mensajes útiles: violación de clave foránea →
  400 con «revise las referencias», llave duplicada → 409 con el `mensajeConflicto` del módulo, y
  cualquier otro fallo → 500 sin filtrar el stack.

## Campos derivados

Ningún cliente envía resultados académicos:

| Campo | Quién lo calcula |
|---|---|
| `calificaciones.promedio_final` | MySQL (columna generada, 30/30/40). |
| `calificaciones.estado` | `ServicioCalificaciones` (notas + asistencia). |
| `matriculas.id_curso`, `periodo` | `ServicioMatriculas`, a partir de la sección. |
| `matriculas.nota`, `estado` | `ServicioMatriculas.sincronizarConCalificacion` y `recalcularEstado`. |
| `secciones.cuposOcupados` | `SeccionDAO` con un `COUNT` en el JOIN. |

Los cuatro primeros se bloquean en Jackson con `READ_ONLY`; si alguien los manda en el JSON, se ignoran.

## Pruebas

`mvn clean test` ejecuta 77 pruebas. Las de `universidad.servicio` montan los DAO como dobles de
Mockito a través del fixture `Escenario`, que deja preparado el camino feliz (estudiante, curso,
sección, periodo y matrícula) y expone métodos para romper una regla cada vez
(`periodoEn`, `cursoDeOtraCarrera`, `ocupacionDeSeccion`, `controlesDeAsistencia`, `matriculasEnCurso`).
Así se prueba la lógica sin tocar MySQL; las consultas SQL reales se verifican contra la base de
demostración con `db/seed/comprobar-demo.sql`.

La otra mitad —que el SQL, los permisos y el JSON funcionen juntos— se verifica sobre una base aparte
creada por `scripts/preparar-base-pruebas.sh` (`universidad_e2e`, nunca `universidad`) y recorrida por
`scripts/verificar-api-e2e.sh`: 202 comprobaciones de las reglas, la matriz de permisos por rol, las
validaciones de forma, los filtros, las fechas ISO y el borrado en cascada. Sale `TODO OK` con código 0
y deja la base limpia de los datos que ella misma creó.

## Decisiones tomadas

- **Servicios en el paquete `servicio`, no en los controllers.** Dos interfaces necesitaban las mismas
  reglas y los mensajes; duplicarlos en `ModulosCrud` y `ModulosSwing` ya había causado diferencias.
- **La sección manda sobre la matrícula.** Antes el formulario elegía curso y periodo a mano y podían
  contradecir al grupo; ahora se derivan.
- **Reglas en la base solo cuando MySQL puede sostenerlas.** El `CHECK` que exigía ficha a cada cuenta
  se rechazó con el error 3823 (columna usada en una FK) y quedó en la aplicación, documentado en
  `db/init/01-schema.sql`.
- **Sin nube.** Nada de Netlify, `localStorage` con contraseñas ni despliegues: el proyecto es un
  sistema local de tres procesos (app, Swing y MySQL) en la misma máquina.
