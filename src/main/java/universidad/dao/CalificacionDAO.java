package universidad.dao;

import java.util.List;
import universidad.modelo.Calificacion;

public interface CalificacionDAO extends CRUD<Calificacion> {

    List<Calificacion> listarPorSeccion(int idSeccion);

    List<Calificacion> listarPorEstudiante(int idEstudiante);

    /** Calificación única por estudiante y sección (restricción unica_inscripcion). */
    Calificacion leerPorEstudianteYSeccion(int idEstudiante, int idSeccion);
}
