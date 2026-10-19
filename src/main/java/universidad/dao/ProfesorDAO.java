package universidad.dao;

import java.util.List;
import universidad.modelo.Profesor;

public interface ProfesorDAO extends CRUD<Profesor> {

    List<Profesor> listarPorFacultad(int idFacultad);
}
