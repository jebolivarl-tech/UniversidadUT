package universidad.dao;

import java.util.List;
import universidad.modelo.Seccion;

public interface SeccionDAO extends CRUD<Seccion> {

    List<Seccion> listarPorProfesor(int idProfesor);

    List<Seccion> listarPorPeriodo(int idPeriodo);

    /** Secciones de los cursos en los que el estudiante tiene matrícula. */
    List<Seccion> listarPorEstudiante(int idEstudiante);
}
