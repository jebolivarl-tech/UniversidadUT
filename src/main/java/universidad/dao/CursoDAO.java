package universidad.dao;

import java.util.List;
import universidad.modelo.Curso;

/**
 * DAO de la tabla cursos: un curso pertenece a una carrera y lo dicta un profesor.
 */
public interface CursoDAO extends CRUD<Curso> {

    List<Curso> listarPorCarrera(int idCarrera);

    List<Curso> listarPorProfesor(int idProfesor);
}
