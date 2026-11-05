package universidad.dao;

import universidad.modelo.Carrera;

/**
 * DAO de la tabla carreras: cada carrera pertenece a una facultad. No necesita consultas
 * especiales, así que aquí basta con lo que trae {@link CRUD}.
 */
public interface CarreraDAO extends CRUD<Carrera> {
}
