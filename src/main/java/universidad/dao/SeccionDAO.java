package universidad.dao;

import java.util.List;
import universidad.modelo.Seccion;

public interface SeccionDAO extends CRUD<Seccion> {

    List<Seccion> listarPorProfesor(int idProfesor);

    List<Seccion> listarPorPeriodo(int idPeriodo);

    /** Secciones de los cursos en los que el estudiante tiene matrícula. */
    List<Seccion> listarPorEstudiante(int idEstudiante);

    /**
     * Secciones que el estudiante aún puede inscribir: curso de su carrera, periodo ACTIVO y sin
     * matrícula previa. Es la lista que muestra el formulario de matrícula.
     */
    List<Seccion> listarInscribiblesPara(int idEstudiante);
}
