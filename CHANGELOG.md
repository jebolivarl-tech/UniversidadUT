# Cambios

Formato de [Mantener un CHANGELOG](https://keepachangelog.com/es-ES/1.1.0/); las versiones van por fecha
porque el proyecto lo sostienen tres personas en una clase, no un pipeline de releases.

## [Sin publicar] — 2026-10-19

### Añadido

- Capa de dominio `universidad.servicio` con `ReglasAcademicas`, `ServicioMatriculas`,
  `ServicioSecciones`, `ServicioAsistencias`, `ServicioCalificaciones`, `Referencias` y
  `ReglaVioladaException`. La API web y el cliente Swing usan ahora las mismas reglas.
- Reglas académicas que antes no existían: tope de 20 créditos por periodo, periodo abierto para
  inscribir o cambiar de sección, curso de la carrera del estudiante, cupo disponible, un control por
  día, fechas dentro del periodo, pérdida del curso por debajo del 75 % de asistencia, la nota
  definitiva sincronizada con la matrícula y el bloqueo de un curso que todavía tiene matrículas.
- Pruebas unitarias de las reglas con dobles de Mockito (`universidad.servicio.*Test` y el fixture
  `Escenario`): de 32 a 77 pruebas.
- Migraciones numeradas `db/migrations/001_seguridad_y_fk.sql` y `002_reglas_academicas.sql`, con la
  pausa documentada para las matrículas que no encuentran sección.
- Datos de demostración `db/seed/datos-demo.sql` y su verificación `db/seed/comprobar-demo.sql`, que
  repite en SQL las reglas de los servicios.
- Endpoints `GET /api/secciones/{id}/cupos` y `GET /api/estudiantes/{id}/secciones-inscribibles`.
- `CrearUsuario` acepta el id de la ficha del rol y rechaza cuentas sin estudiante o profesor.
- Ganchos `despuesEscribir` y `antesEliminar` en `FormularioCRUD`, los mismos de `CrudController`.
- Guiones de verificación compartidos: `scripts/preparar-base-pruebas.sh` monta la base de pruebas
  `universidad_e2e` (esquema + semilla + tres cuentas) y `scripts/verificar-api-e2e.sh` recorre 202
  comprobaciones de reglas, permisos por rol, validaciones, filtros, fechas ISO y borrado en cascada.
  Ninguno toca la base real.
- El formulario de matrícula pregunta al servidor qué grupos puede inscribir el estudiante elegido
  (`inscribiblesDe` en la configuración del módulo): la lista ya no es el catálogo completo de secciones.
- Documentación: `docs/reglas-de-negocio.md`, `docs/arquitectura.md`,
  `docs/instalacion-paso-a-paso.md` y este archivo.

### Cambiado

- Una matrícula se inscribe en una **sección**; el curso, el periodo y el estado se derivan de ella y ya
  no se escriben a mano en ningún formulario.
- `matriculas.nota` pasa a `decimal(4,2)` y `matriculas.periodo` a `varchar(50)`: las notas se publican
  con dos decimales y los nombres de periodo ya no se truncan.
- `calificaciones.promedio_final` se recalcula en MySQL con `COALESCE` y `ROUND(..., 2)`, igual que Java.
- La ocupación real (`cuposOcupados`), la sección y el estado académico aparecen en las tablas de la web,
  en `mi.html` y en el escritorio.
- La comparación de aulas ignora mayúsculas, como el índice único de la base.
- Los estados derivados se bloquean en Jackson con `READ_ONLY`: `estado`, `promedioFinal`,
  `cuposOcupados`, `nombreCurso` y `nombrePeriodo` no los decide el cliente.

### Corregido

- `mi.html` pintaba `[object Object]` en todas las cabeceras de tabla al pasar el objeto de columna en
  lugar de su etiqueta.
- La fecha futura de una asistencia se rechazaba dos veces (en `Validaciones` y en el servicio); se queda
  solo la regla del periodo.
- `CrearUsuario` deducía mal el nombre de la columna de la ficha y creaba cuentas de estudiante o
  profesor sin ficha, que luego la API no sabía mostrar.
- Borrar un curso con estudiantes matriculados se aceptaba con 200 y la base se llevaba por delante
  sus grupos y sus matrículas (`fk_matricula_curso … ON DELETE CASCADE`). Ahora
  `ServicioSecciones.validarEliminacionDeCurso` lo corta con 409 en la web y en el escritorio, y la
  verificación end-to-end lo comprueba.
- El borrado que la base bloquea por clave foránea (MySQL 1451) respondía 500 sin explicación; ahora
  es 409 con el mensaje de la regla (`Http.esBorradoBloqueado`).
- `GET /api/estudiantes/{id}/secciones-inscribibles` ofrecía matrículas que la base rechaza: exclucía
  la misma sección en lugar del mismo curso y periodo (`uq_matricula`) e ignoraba los grupos llenos y
  el tope de 20 créditos. Ahora solo ofrece lo que el estudiante puede inscribir, y el servicio lo
  refuerza con el mensaje «El estudiante ya cursa «Cálculo» en 2026-2…».

### Eliminado

- `db/001_seguridad_y_fk.sql` en la raíz: se mudó a `db/migrations/` con numeración y sin tocar datos.

## [0.3] — 2026-10-19

Interfaz web completa de los nueve módulos, identidad visual con el logo oficial de la UT (verde
`#0b6e4f` y oro `#f0b429`), acento e icono por módulo, y arreglo de la codificación UTF-8 real en todas
las páginas y recursos servidos.

## [0.2] — 2026-10-18

Base de datos con claves foráneas y RESTRICT, sesiones en servidor con PBKDF2, matriz de permisos por
rol aplicada en el servidor y cliente Swing alineado con los mismos módulos.

## [0.1] — 2026-10-14

Primer volcado del proyecto universitario (Javalin + Swing + MySQL) y su esquema original en
`tablas-MySql/`. Las variantes de despliegue en la nube y el almacenamiento de contraseñas en el
navegador se retiraron al cerrar el alcance en localhost.
