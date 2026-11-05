package universidad.dao;

import java.util.List;
import universidad.modelo.Calificacion;

/**
 * DAO de la tabla calificaciones: dos parciales, examen final y un promedio que MySQL calcula
 * solo (30%, 30% y 40%). Las notas empiezan vacías porque un cero significa nota presentada.
 */
public interface CalificacionDAO extends CRUD<Calificacion> {

    List<Calificacion> listarPorSeccion(int idSeccion);

    List<Calificacion> listarPorEstudiante(int idEstudiante);

    /** Hay una sola fila por estudiante y sección (restricción unica_inscripcion); devuelve null si aún no existe. */
    Calificacion leerPorEstudianteYSeccion(int idEstudiante, int idSeccion);
}
