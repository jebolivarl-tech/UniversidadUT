-- Migración recomendada (MySQL 8). Ejecutar UNA vez sobre la base `universidad`.
-- 1) Al borrar una carrera con estudiantes, la BD debe RECHAZAR el borrado (la API responde 409)
--    en lugar de borrar en cascada a todos los estudiantes y sus usuarios.
ALTER TABLE estudiantes DROP FOREIGN KEY fk_estudiante_carrera;
ALTER TABLE estudiantes
  ADD CONSTRAINT fk_estudiante_carrera FOREIGN KEY (id_carrera)
  REFERENCES carreras (id_carrera) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 2) Una carrera con facultad: evitar que borrar una facultad arrastre carreras y estudiantes.
ALTER TABLE carreras DROP FOREIGN KEY fk_carrera_facultad;
ALTER TABLE carreras
  ADD CONSTRAINT fk_carrera_facultad FOREIGN KEY (id_facultad)
  REFERENCES facultades (id_facultad) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 3) Las contraseñas deben guardarse con hash. Después de aplicar este script ejecute:
--      mvn exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords
--    (o simplemente deje que cada usuario inicie sesión: la contraseña se convierte a hash automáticamente).
--    Tras migrar, cambie las contraseñas de ejemplo (admin123, etc.) por contraseñas reales.
