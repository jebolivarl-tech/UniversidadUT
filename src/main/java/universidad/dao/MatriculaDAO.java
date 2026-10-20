package universidad.dao;

import java.util.List;
import universidad.modelo.Matricula;

public interface MatriculaDAO extends CRUD<Matricula> {

    List<Matricula> listarPorEstudiante(int idEstudiante);

    List<Matricula> listarPorCurso(int idCurso);

    List<Matricula> listarPorSeccion(int idSeccion);

    List<Matricula> listarPorEstudianteYPeriodo(int idEstudiante, String periodo);

    /** Cuántos cupos está ocupando hoy la sección. Base del tope de cupo del servicio. */
    int contarPorSeccion(int idSeccion);

    Matricula leerPorEstudianteYSeccion(int idEstudiante, int idSeccion);
}
