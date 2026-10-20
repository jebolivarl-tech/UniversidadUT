# Reglas de negocio

Las reglas académicas viven en `src/main/java/universidad/servicio/` y las ejecutan **las dos
interfaces** (la API web y el cliente Swing). Ninguna página ni formulario decide un resultado
académico: el cliente envía datos y el servidor deriva lo que corresponda.

Los números que definen la evaluación están en un solo lugar, [`ReglasAcademicas`](../src/main/java/universidad/servicio/ReglasAcademicas.java):

| Constante | Valor | Qué significa |
|---|---|---|
| `PORCENTAJE_MINIMO_ASISTENCIA` | 0.75 | Por debajo se pierde el derecho a evaluación. |
| `CONTROLES_MINIMOS_PARA_PERDER_POR_ASISTENCIA` | 4 | Con menos de cuatro llamados nadie se sentencia por inasistencia. |
| `CREDITOS_MAXIMOS_POR_PERIODO` | 20 | Tope de créditos acumulables en un mismo periodo. |
| `Calificacion.PROMEDIO_APROBATORIO` | 3.0 | Nota mínima para aprobar, en escala de 0.0 a 5.0. |

Un cambio de reglamento (por ejemplo subir la asistencia al 80 %) se hace editando estos cuatro
números y las pruebas de `universidad.servicio`; no hay que recorrer los formularios.

## Escala y pesos

- Notas entre **0.0 y 5.0**, dos decimales (`ReglasAcademicas.redondearNota`).
- **Promedio = 30 % parcial 1 + 30 % parcial 2 + 40 % examen final.** Lo calcula MySQL con la columna
  generada `calificaciones.promedio_final` y, a la vez, Java con `Calificacion.promedio(...)` para poder
  decidir el estado antes de escribir. Las dos fórmulas están en `db/init/01-schema.sql` y en
  `modelo/Calificacion.java`: si cambia una, cambia la otra.
- Un campo **vacío no es un cero**: mientras falte una de las tres notas el estado es `EN_CURSO`.

## Matrículas — `ServicioMatriculas`

| Regla | Mensaje al usuario |
|---|---|
| La **sección** decide el curso y el periodo: el formulario solo pide estudiante, sección y (opcional) nota. | — (derivado, no hay mensaje) |
| Solo se inscribe en un periodo `ACTIVO`. Cambiar de sección cuenta como inscripción nueva y exige el mismo periodo abierto. | «El periodo 2026-1 está CERRADO: no se admiten matrículas ni cambios de sección.» |
| El curso tiene que ser de la **carrera del estudiante**. | «El curso «X» no pertenece a la carrera del estudiante: no puede matricularlo.» |
| Nadie se matricula dos veces en la misma sección. | «El estudiante ya está matriculado en esa sección.» |
| Tampoco dos veces en el **mismo curso y periodo**, aunque cambie de grupo (`uq_matricula`). | «El estudiante ya cursa «Cálculo» en 2026-2: elija un curso distinto o cambie de periodo.» |
| El **cupo** debe estar disponible. | «La sección … está llena (30/30 cupos).» |
| Los créditos del periodo no pueden pasar de 20, contando los ya inscritos. | «El estudiante llegaría a 21 créditos en 2026-2 y el máximo por periodo es de 20.» |
| El estado se deriva: **primero la asistencia, después la nota**. | — |

Estados posibles: `EN_CURSO` (sin nota), `APROBADO` (≥ 3.0), `REPROBADO` (< 3.0) y
`PERDIDO_POR_FALTA_DE_ASISTENCIA`, que manda sobre la nota: se puede tener 4.8 y perder el curso.

## Asistencias — `ServicioAsistencias`

- Solo se pasa lista a **estudiantes matriculados** en esa sección.
- La fecha tiene que caer entre `fecha_inicio` y `fecha_fin` del periodo de la sección.
- **Un registro por estudiante, sección y día** (`uq_asistencia_dia` en la base refuerza la regla).
- Estados: `ASISTIO`, `FALTO`, `TARDANZA`, `JUSTIFICADO`. La tardanza y la falta justificada
  **no descuentan**; solo `FALTO` baja el porcentaje.

## Calificaciones — `ServicioCalificaciones`

- Se puede calificar con notas parciales vacías; el promedio de MySQL vale lo digitado.
- No se califica a quien no está matriculado en la sección.
- Al guardar la definitiva completa, `propagarNotaDefinitiva` **copia el promedio a la matrícula** y
  recalcula su estado. Es el único punto donde la nota de la matrícula cambia por evaluación.

## Secciones — `ServicioSecciones`

- Curso, profesor y periodo tienen que existir.
- El mismo curso no puede tener dos grupos en la **misma aula y el mismo periodo** (la comparación
  ignora mayúsculas, igual que el índice único de la base).
- El cupo máximo no puede bajar de la cantidad de matriculados actuales.
- Una sección con estudiantes **no se borra**: antes hay que retirar las matrículas
  (`fk_matricula_seccion … ON DELETE RESTRICT` respalda la regla en la base).
- Un curso con matrículas **tampoco se borra**. La base sí lo permitiría en silencio:
  `fk_matricula_curso … ON DELETE CASCADE` arrastra los grupos y, con ellos, las matrículas ya
  cargadas de los estudiantes. `validarEliminacionDeCurso` cierra esa puerta con un 409
  («El curso «X» tiene 3 matrícula(s) en sus grupos: elimine primero las matrículas.») y es la misma
  que usan el panel web y el de escritorio.

## Qué se ofrece para inscribirse — `GET /api/estudiantes/{id}/secciones-inscribibles`

El formulario de matrículas no muestra todos los grupos y espera el error: pregunta al servidor y
ofrece solo los que este estudiante **sí** puede elegir. La lista sale de `SeccionDAOImpl` (carrera
coincidente, periodo `ACTIVO`, sin matrícula previa en ese curso y periodo, cupo no lleno) y
`ServicioSecciones.inscribiblesPara` quita encima los que pasarían del tope de créditos del periodo.
Lo que no aparece aquí no es una opción que el usuario deba descubrir a golpes de 409.

`GET /api/secciones/{id}/cupos` hace lo mismo con el cupo: `ocupados`, `disponibles` y la descripción
del grupo los calcula el servidor a partir de las matrículas reales.

## Cuentas de acceso — `Validaciones` y `CrearUsuario`

- Toda cuenta no administrativa tiene su **ficha**: `ESTUDIANTE` → `id_estudiante`,
  `PROFESOR` → `id_profesor`, `ADMINISTRATIVO` → ninguna.
- MySQL no permite un `CHECK` sobre columnas que participan en una clave foránea (error 3823), así que
  esta regla es de aplicación, no de esquema. Por eso la comprueban también los dos caminos posibles:
  la API (`Validaciones.esCuentaValida`) y la consola (`universidad.seguridad.CrearUsuario`).
- La contraseña se guarda con PBKDF2 y **nunca** sale por la API.

## Dónde se defiende cada regla

| Capa | Qué hace |
|---|---|
| `universidad.servicio` | Regla de negocio y mensaje para el usuario (`ReglaVioladaException` lleva el 409 o el 400). |
| `web/Validaciones` | Formato: campos vacíos, rangos, longitudes, fechas. No repite reglas académicas. |
| `db/init/01-schema.sql` | Invariantes que la base sí puede garantizar: `UNIQUE`, `CHECK`, `FOREIGN KEY … RESTRICT`, columna generada. |
| `public/modulos.js`, `swing/` | Solo presentación y orientación del formulario; el servidor repite todo. |
