package universidad.servicio;

import universidad.dao.AsistenciaDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Calificacion;
import universidad.modelo.Estudiante;
import universidad.modelo.Seccion;

/**
 * Evaluación continua: dos parciales al 30 % y el examen final al 40 %; se aprueba con 3.0. Una
 * inasistencia no justificada puede hacer perder el curso aunque el promedio sea bueno: la asistencia
 * pesa más que la nota. Desde aquí se sincroniza la definitiva de la matrícula, para que no queden
 * dos versiones del mismo resultado. Los datos se leen y guardan con los DAO, las clases que usan
 * JDBC y SQL contra MySQL.
 */
public class ServicioCalificaciones {

    private final EstudianteDAO estudiantes;
    private final SeccionDAO secciones;
    private final MatriculaDAO matriculas;
    private final AsistenciaDAO asistencias;
    private final ServicioMatriculas servicioMatriculas;

    public ServicioCalificaciones(EstudianteDAO estudiantes, SeccionDAO secciones, MatriculaDAO matriculas,
            AsistenciaDAO asistencias, ServicioMatriculas servicioMatriculas) {
        this.estudiantes = estudiantes;
        this.secciones = secciones;
        this.matriculas = matriculas;
        this.asistencias = asistencias;
        this.servicioMatriculas = servicioMatriculas;
    }

    /** Solo se califica al estudiante matriculado en esa sección; si está bien, calcula promedio y estado. */
    public void preparar(Calificacion calificacion) {
        Estudiante estudiante = Referencias.estudiante(estudiantes, calificacion.getIdEstudiante());
        Seccion seccion = Referencias.seccion(secciones, calificacion.getIdSeccion());
        if (matriculas.leerPorEstudianteYSeccion(estudiante.getIdEstudiante(), seccion.getIdSeccion()) == null) {
            throw ReglaVioladaException.deConflicto(estudiante.getNombre() + " " + estudiante.getApellido()
                    + " no está matriculado en la sección " + seccion.describir()
                    + ": regístrelo antes de cargarle notas.");
        }
        boolean pierdePorAsistencia = ReglasAcademicas.pierdePorAsistencia(
                asistencias.listarPorEstudianteYSeccion(calificacion.getIdEstudiante(), calificacion.getIdSeccion()));
        calificacion.setPromedioFinal(ReglasAcademicas.redondearNota(Calificacion.promedio(
                calificacion.getNotaParcial1(), calificacion.getNotaParcial2(), calificacion.getNotaExamenFinal())));
        calificacion.setEstado(Calificacion.estado(calificacion.getNotaParcial1(),
                calificacion.getNotaParcial2(), calificacion.getNotaExamenFinal(), pierdePorAsistencia));
    }

    /** La definitiva se vuelca en la matrícula al guardar la calificación, con la misma nota. */
    public void propagarNotaDefinitiva(Calificacion calificacion) {
        servicioMatriculas.sincronizarConCalificacion(calificacion);
    }
}
