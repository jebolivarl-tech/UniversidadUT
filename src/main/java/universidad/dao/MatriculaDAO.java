package universidad.dao;

import java.util.List;
import universidad.modelo.Matricula;

/**
 * DAO de la tabla matriculas: el registro que amarra un estudiante con un curso, una sección y
 * un periodo. Es el centro del negocio: ocupar cupo aquí es lo que habilita notas y asistencia.
 */
public interface MatriculaDAO extends CRUD<Matricula> {

    List<Matricula> listarPorEstudiante(int idEstudiante);

    List<Matricula> listarPorCurso(int idCurso);

    List<Matricula> listarPorSeccion(int idSeccion);

    /** Matrículas de un estudiante en un periodo: el servicio la usa para no repetir un curso y para contar los créditos. */
    List<Matricula> listarPorEstudianteYPeriodo(int idEstudiante, String periodo);

    /** Cuántas matrículas tiene la sección, o sea cuántos cupos ocupa hoy; el servicio lo compara con el cupo máximo. */
    int contarPorSeccion(int idSeccion);

    /** Devuelve null si no hay matrícula; el servicio bloquea así la duplicada y comprueba que el estudiante está inscrito. */
    Matricula leerPorEstudianteYSeccion(int idEstudiante, int idSeccion);
}
