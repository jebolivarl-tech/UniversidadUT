package universidad.dao;

import universidad.modelo.Facultad;

/**
 * DAO de la tabla facultades, la unidad más grande de la universidad: debajo de ella están
 * las carreras. Solo usa lo que trae {@link CRUD}.
 */
public interface FacultadDAO extends CRUD<Facultad> {
}
