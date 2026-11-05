package universidad.dao;

import java.util.List;
import universidad.modelo.Profesor;

/**
 * DAO de la tabla profesores: cada profesor adscrito a una facultad, igual que las carreras.
 */
public interface ProfesorDAO extends CRUD<Profesor> {

    List<Profesor> listarPorFacultad(int idFacultad);
}
