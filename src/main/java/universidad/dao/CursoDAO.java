package universidad.dao;

import java.util.List;
import universidad.modelo.Curso;

public interface CursoDAO extends CRUD<Curso> {

    List<Curso> listarPorCarrera(int idCarrera);

    List<Curso> listarPorProfesor(int idProfesor);
}
