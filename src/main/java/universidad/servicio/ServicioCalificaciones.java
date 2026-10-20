package universidad.servicio;

import universidad.dao.AsistenciaDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Calificacion;
import universidad.modelo.Estudiante;
import universidad.modelo.Seccion;

/**
 * Evaluación continua: tres momentos (30 % + 30 % + 40 %), se aprueba con 3.0 y la inasistencia no
 * justificada puede hacer perder el curso aunque la nota acompañe. La nota definitiva de la
 * matrícula se sincroniza desde aquí, para que no haya dos versiones del mismo resultado.
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

    /** Comprueba que se puede calificar y calcula promedio y estado derivados. */
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

    /** Volcar la definitiva sobre la matrícula, después de guardar la calificación. */
    public void propagarNotaDefinitiva(Calificacion calificacion) {
        servicioMatriculas.sincronizarConCalificacion(calificacion);
    }
}
