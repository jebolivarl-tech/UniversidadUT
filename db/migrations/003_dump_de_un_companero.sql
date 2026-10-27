-- 003 · Adaptar el volcado de un compañero al esquema que usa el proyecto.
--
-- Cuándo se necesita: cuando llega un `universidad.sql` hecho con `mysqldump` desde una base que
-- todavía no tiene las reglas académicas (es decir, antes de aplicar 001 y 002). Los pasos 001 y 002
-- ya convierten `matriculas`, `secciones`, `asistencias`, `calificaciones`, `cursos`, `carreras` y
-- `periodos`; este archivo se ocupa de lo que esas dos migraciones no pueden tocar porque son tablas
-- o columnas que el proyecto no conoce.
--
-- Orden completo sobre una base `universidad_companero` ya cargada con el volcado:
--   scripts/adaptar-base-del-companero.sh   (hace los tres pasos de una sola pasada)
-- o a mano, dentro de la base del volcado:
--   001_seguridad_y_fk.sql → 002_reglas_academicas.sql → 003_dump_de_un_companero.sql
--
-- Se ejecuta UNA vez. Si se repite, los `ALTER` de `usuarios` fallan porque el trabajo ya está hecho.

-- 1 · Tablas sueltas que arrastra el Excel de turno. El proyecto usa estas once y ninguna más:
--     facultades, periodos_academicos, profesores, carreras, estudiantes, cursos, secciones,
--     matriculas, asistencias, calificaciones y usuarios. Lo demás es basura del volcado.
DROP TABLE IF EXISTS `libro1`;

-- 2 · `ADMINISTRADOR` era un rol intermedio que la aplicación ya no reconoce
--     (Sesion.ADMINISTRATIVO es el que autoriza los módulos de edición).
UPDATE `usuarios` SET `rol` = 'ADMINISTRATIVO' WHERE `rol` = 'ADMINISTRADOR';

ALTER TABLE `usuarios`
  MODIFY COLUMN `rol` enum('ADMINISTRATIVO','ESTUDIANTE','PROFESOR') NOT NULL;

-- 3 · `id_referencia` guardaba a medias la ficha de la cuenta. El proyecto usa las dos columnas
--     explícitas (`id_estudiante`, `id_profesor`), así que se rellenan antes de borrar la antigua y
--     se sueltan las referencias que no apuntan a ninguna ficha: sin eso, el `ADD CONSTRAINT` del
--     paso 4 falla y la cuenta queda inservible de todos modos (Validaciones.esCuentaValida la rechaza).
UPDATE `usuarios` SET `id_estudiante` = `id_referencia`
 WHERE `rol` = 'ESTUDIANTE' AND `id_estudiante` IS NULL AND `id_referencia` IS NOT NULL;

UPDATE `usuarios` SET `id_profesor` = `id_referencia`
 WHERE `rol` = 'PROFESOR' AND `id_profesor` IS NULL AND `id_referencia` IS NOT NULL;

ALTER TABLE `usuarios` DROP COLUMN `id_referencia`;

UPDATE `usuarios` u LEFT JOIN `estudiantes` e ON e.`id_estudiante` = u.`id_estudiante`
   SET u.`id_estudiante` = NULL
 WHERE u.`id_estudiante` IS NOT NULL AND e.`id_estudiante` IS NULL;

UPDATE `usuarios` u LEFT JOIN `profesores` p ON p.`id_profesor` = u.`id_profesor`
   SET u.`id_profesor` = NULL
 WHERE u.`id_profesor` IS NOT NULL AND p.`id_profesor` IS NULL;

-- 4 · Las cuentas dejan de ser un registro suelto: borrar al estudiante o al profesor cierra su cuenta,
--     y el índice del usuario se nombra como en db/init/01-schema.sql.
ALTER TABLE `usuarios`
  ADD CONSTRAINT `fk_usuario_estudiante` FOREIGN KEY (`id_estudiante`)
      REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_usuario_profesor` FOREIGN KEY (`id_profesor`)
      REFERENCES `profesores` (`id_profesor`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `usuarios` RENAME INDEX `username` TO `idx_username`;

-- 5 · Comprobaciones. Deben salir vacías:
--       SELECT table_name FROM information_schema.tables
--        WHERE table_schema = DATABASE()
--          AND table_name NOT IN ('facultades','periodos_academicos','profesores','carreras',
--                                 'estudiantes','cursos','secciones','matriculas','asistencias',
--                                 'calificaciones','usuarios');
--       SELECT username, rol FROM usuarios
--        WHERE (rol = 'ESTUDIANTE' AND id_estudiante IS NULL)
--           OR (rol = 'PROFESOR'    AND id_profesor    IS NULL);
--     Y después, el recorrido completo de reglas: db/seed/comprobar-demo.sql.

-- 6 · Las contraseñas del volcado ya están en formato `pbkdf2_sha256$iteraciones$salt$hash`, el que lee
--     `PasswordHasher`, así que se puede conservar la cuenta del compañero si se conoce su clave. Si no,
--     borre la fila y cree la suya (las cuentas nunca se insertan a mano con la clave en texto plano):
--       java -cp target/app.jar universidad.seguridad.CrearUsuario admin SuClave1234567 ADMINISTRATIVO
