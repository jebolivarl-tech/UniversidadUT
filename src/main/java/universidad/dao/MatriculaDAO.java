package universidad.dao;

import java.util.List;
import universidad.modelo.Matricula;

public interface MatriculaDAO extends CRUD<Matricula> {

    List<Matricula> listarPorEstudiante(int idEstudiante);

    List<Matricula> listarPorCurso(int idCurso);
}
