package universidad.dao;

import java.sql.Date;
import java.util.List;
import universidad.modelo.Asistencia;

/**
 * DAO de la tabla asistencias: cada registro es el control que un profesor pasa a un estudiante
 * de una sección en una fecha, con su estado (ASISTIO, FALTO, TARDANZA o JUSTIFICADO).
 * Además de las cinco operaciones de {@link CRUD}, expone listas filtradas para armar los reportes de asistencia.
 */
public interface AsistenciaDAO extends CRUD<Asistencia> {

    List<Asistencia> listarPorSeccion(int idSeccion);

    List<Asistencia> listarPorSeccionYFecha(int idSeccion, Date fecha);

    List<Asistencia> listarPorEstudiante(int idEstudiante);

    /** Los controles de un estudiante en una sección; con esto el servicio decide si pierde el curso por faltas. */
    List<Asistencia> listarPorEstudianteYSeccion(int idEstudiante, int idSeccion);

    /** Cuántos controles de una sección tienen un estado dado (ASISTIO, FALTO, etc.). */
    int contarPorSeccionYEstado(int idSeccion, String estado);
}
