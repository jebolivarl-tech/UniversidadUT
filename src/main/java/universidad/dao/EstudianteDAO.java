package universidad.dao;

import universidad.modelo.Estudiante;

/**
 * DAO de la tabla estudiantes: datos personales y carrera de cada alumno. Con las cinco
 * operaciones de {@link CRUD} es suficiente; las búsquedas por curso o sección viven en
 * los DAO de esas tablas, que son las que tienen la relación.
 */
public interface EstudianteDAO extends CRUD<Estudiante> {
}
