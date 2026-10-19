package universidad.dao;

import java.sql.Date;
import java.util.List;
import universidad.modelo.Asistencia;

public interface AsistenciaDAO extends CRUD<Asistencia> {

    List<Asistencia> listarPorSeccion(int idSeccion);

    List<Asistencia> listarPorSeccionYFecha(int idSeccion, Date fecha);

    List<Asistencia> listarPorEstudiante(int idEstudiante);

    int contarPorSeccionYEstado(int idSeccion, String estado);
}
