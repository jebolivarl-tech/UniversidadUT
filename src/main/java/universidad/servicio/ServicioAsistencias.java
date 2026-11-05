package universidad.servicio;

import java.sql.Date;
import universidad.dao.AsistenciaDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Asistencia;
import universidad.modelo.Estudiante;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Seccion;

/**
 * Pasa lista y corrige registros de asistencia. Solo se marca asistencia de quien está matriculado
 * en la sección, en una fecha que cae dentro del periodo, y una sola vez por día. Cada registro se
 * guarda con un DAO, la clase que escribe en MySQL usando JDBC y SQL.
 */
public class ServicioAsistencias {

    private final AsistenciaDAO asistencias;
    private final EstudianteDAO estudiantes;
    private final SeccionDAO secciones;
    private final MatriculaDAO matriculas;
    private final PeriodoAcademicoDAO periodos;

    public ServicioAsistencias(AsistenciaDAO asistencias, EstudianteDAO estudiantes, SeccionDAO secciones,
            MatriculaDAO matriculas, PeriodoAcademicoDAO periodos) {
        this.asistencias = asistencias;
        this.estudiantes = estudiantes;
        this.secciones = secciones;
        this.matriculas = matriculas;
        this.periodos = periodos;
    }

    /** Revisa todo el registro antes de guardarlo; si algo falla, nada se escribe. */
    public void preparar(Asistencia asistencia) {
        asistencia.setEstado(asistencia.getEstado().toUpperCase());
        Estudiante estudiante = Referencias.estudiante(estudiantes, asistencia.getIdEstudiante());
        Seccion seccion = Referencias.seccion(secciones, asistencia.getIdSeccion());
        asegurarMatriculado(estudiante, seccion);
        asegurarFechaDelPeriodo(asistencia, seccion);
        asegurarUnRegistroPorDia(asistencia, seccion);
    }

    /** Nadie que no esté matriculado en la sección puede figurar en la lista de ese grupo. */
    private void asegurarMatriculado(Estudiante estudiante, Seccion seccion) {
        Matricula matricula = matriculas.leerPorEstudianteYSeccion(
                estudiante.getIdEstudiante(), seccion.getIdSeccion());
        if (matricula == null) {
            throw ReglaVioladaException.deConflicto(estudiante.getNombre() + " " + estudiante.getApellido()
                    + " no está matriculado en la sección " + seccion.describir()
                    + " y por eso no se le puede pasar lista.");
        }
    }

    /** La fecha del control debe caer entre el inicio y el fin del periodo al que pertenece la sección. */
    private void asegurarFechaDelPeriodo(Asistencia asistencia, Seccion seccion) {
        PeriodoAcademico periodo = Referencias.periodo(periodos, seccion.getIdPeriodo());
        Date fecha = asistencia.getFecha();
        if (periodo.getFechaInicio() != null && periodo.getFechaFin() != null
                && (fecha.before(periodo.getFechaInicio()) || fecha.after(periodo.getFechaFin()))) {
            throw ReglaVioladaException.deConflicto("La fecha " + fecha
                    + " está fuera del periodo " + periodo.getNombre() + " ("
                    + periodo.getFechaInicio() + " a " + periodo.getFechaFin() + ").");
        }
    }

    /** Un estudiante no puede tener dos controles el mismo día: se consulta la lista antes de escribir. */
    private void asegurarUnRegistroPorDia(Asistencia asistencia, Seccion seccion) {
        boolean repetida = asistencias.listarPorSeccionYFecha(seccion.getIdSeccion(), asistencia.getFecha())
                .stream()
                .anyMatch(otra -> otra.getIdEstudiante() == asistencia.getIdEstudiante()
                        && otra.getIdAsistencia() != asistencia.getIdAsistencia());
        if (repetida) {
            throw ReglaVioladaException.deConflicto("Ese estudiante ya tiene un registro de asistencia el "
                    + asistencia.getFecha() + " en esta sección.");
        }
    }
}
