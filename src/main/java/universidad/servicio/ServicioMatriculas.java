package universidad.servicio;

import java.sql.SQLException;
import java.util.List;
import universidad.dao.AsistenciaDAO;
import universidad.dao.CursoDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Calificacion;
import universidad.modelo.Curso;
import universidad.modelo.Estudiante;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Seccion;

/**
 * Inscripción de estudiantes en secciones. El curso y el periodo de una matrícula nunca los decide
 * el formulario: siempre se derivan de la sección, para que no haya matrículas que contradigan al
 * grupo en el que están. Cada regla consulta primero el estado real en la base a través de los DAO,
 * las clases que leen y escriben MySQL con JDBC y SQL.
 */
public class ServicioMatriculas {

    private final EstudianteDAO estudiantes;
    private final CursoDAO cursos;
    private final SeccionDAO secciones;
    private final PeriodoAcademicoDAO periodos;
    private final MatriculaDAO matriculas;
    private final AsistenciaDAO asistencias;

    public ServicioMatriculas(EstudianteDAO estudiantes, CursoDAO cursos, SeccionDAO secciones,
            PeriodoAcademicoDAO periodos, MatriculaDAO matriculas, AsistenciaDAO asistencias) {
        this.estudiantes = estudiantes;
        this.cursos = cursos;
        this.secciones = secciones;
        this.periodos = periodos;
        this.matriculas = matriculas;
        this.asistencias = asistencias;
    }

    /** Aplica las reglas de inscripción y deja la matrícula lista para guardar, sin escribir nada todavía. */
    public void preparar(Matricula matricula) {
        Estudiante estudiante = Referencias.estudiante(estudiantes, matricula.getIdEstudiante());
        Seccion seccion = Referencias.seccion(secciones, matricula.getIdSeccion());
        Curso curso = Referencias.curso(cursos, seccion.getIdCurso());
        PeriodoAcademico periodo = Referencias.periodo(periodos, seccion.getIdPeriodo());

        matricula.setIdCurso(seccion.getIdCurso());
        matricula.setPeriodo(periodo.getNombre());

        Matricula previa = matricula.getIdMatricula() == 0
                ? null : matriculas.leerPorId(matricula.getIdMatricula());
        boolean esInscripcionNueva = previa == null || previa.getIdSeccion() != matricula.getIdSeccion();
        if (esInscripcionNueva) {
            // Cambiar de sección cuenta como inscripción nueva: solo se admite con el periodo abierto.
            asegurarPeriodoAbierto(periodo);
            asegurarCarreraCompatible(estudiante, curso);
            asegurarNoRepetida(matricula, curso, periodo);
            asegurarCupoDisponible(seccion, matricula);
        }
        asegurarTopeDeCreditos(estudiante, curso, periodo, matricula);
        recalcularEstado(matricula);
    }

    /** Estado real de la matrícula: la asistencia manda sobre la nota. */
    public void recalcularEstado(Matricula matricula) {
        if (ReglasAcademicas.pierdePorAsistencia(
                asistencias.listarPorEstudianteYSeccion(matricula.getIdEstudiante(), matricula.getIdSeccion()))) {
            matricula.setEstado(Matricula.PERDIDO_POR_FALTA_DE_ASISTENCIA);
            return;
        }
        if (matricula.getNota() == null) {
            matricula.setEstado(Matricula.EN_CURSO);
            return;
        }
        matricula.setEstado(matricula.getNota() >= Calificacion.PROMEDIO_APROBATORIO
                ? Matricula.APROBADO : Matricula.REPROBADO);
    }

    /** Cuántos cupos libres quedan: el máximo de la sección menos los matriculados contados en la base. */
    public int cuposDisponibles(int idSeccion) {
        Seccion seccion = Referencias.seccion(secciones, idSeccion);
        return Math.max(0, seccion.getCupoMaximo() - matriculas.contarPorSeccion(idSeccion));
    }

    /**
     * Cuando las tres notas existen, la matrícula recibe la definitiva y su estado.
     * Devuelve null si el estudiante no tiene inscripción en esa sección o si falta alguna nota.
     */
    public Matricula sincronizarConCalificacion(Calificacion calificacion) {
        Matricula matricula = matriculas.leerPorEstudianteYSeccion(
                calificacion.getIdEstudiante(), calificacion.getIdSeccion());
        if (matricula == null || !definitivaCompleta(calificacion)) {
            return null;
        }
        matricula.setNota(ReglasAcademicas.redondearNota(Calificacion.promedio(calificacion.getNotaParcial1(),
                calificacion.getNotaParcial2(), calificacion.getNotaExamenFinal())));
        recalcularEstado(matricula);
        try {
            matriculas.actualizar(matricula);
        } catch (SQLException ex) {
            // El DAO abre la conexión JDBC y lanza SQLException si el UPDATE falla; aquí se envuelve
            // para que la API devuelva un mensaje propio y no el detalle crudo de la base.
            throw new IllegalStateException("No fue posible sincronizar la nota de la matrícula.", ex);
        }
        return matricula;
    }

    private static boolean definitivaCompleta(Calificacion c) {
        return c.getNotaParcial1() != null && c.getNotaParcial2() != null && c.getNotaExamenFinal() != null;
    }

    /** Con el periodo en INACTIVO o CERRADO no entran inscripciones nuevas ni cambios de sección. */
    private void asegurarPeriodoAbierto(PeriodoAcademico periodo) {
        String estado = periodo.getEstado() == null ? PeriodoAcademico.INACTIVO : periodo.getEstado().toUpperCase();
        if (!PeriodoAcademico.ACTIVO.equals(estado)) {
            throw ReglaVioladaException.deConflicto("El periodo " + periodo.getNombre() + " está " + estado
                    + ": no se admiten matrículas ni cambios de sección.");
        }
    }

    /** Un estudiante solo puede matricularse en cursos de la carrera a la que pertenece. */
    private void asegurarCarreraCompatible(Estudiante estudiante, Curso curso) {
        if (curso.getIdCarrera() != estudiante.getIdCarrera()) {
            throw ReglaVioladaException.deConflicto("El curso «" + curso.getNombre() + "» no pertenece a la carrera "
                    + "del estudiante: no puede matricularlo.");
        }
    }

    /** Nadie queda dos veces en la misma sección, ni repite el curso en el mismo periodo. */
    private void asegurarNoRepetida(Matricula matricula, Curso curso, PeriodoAcademico periodo) {
        Matricula existente = matriculas.leerPorEstudianteYSeccion(
                matricula.getIdEstudiante(), matricula.getIdSeccion());
        if (existente != null && existente.getIdMatricula() != matricula.getIdMatricula()) {
            throw ReglaVioladaException.deConflicto("El estudiante ya está matriculado en esa sección.");
        }
        // uq_matricula no admite dos filas del mismo estudiante, curso y periodo: otro grupo del mismo
        // curso tampoco. Se avisa aquí, con nombres, en lugar de dejar que la base devuelva un 409 opaco.
        boolean otroGrupoDelMismoCurso = matriculas.listarPorEstudianteYPeriodo(
                matricula.getIdEstudiante(), periodo.getNombre()).stream()
                .filter(otra -> otra.getIdMatricula() != matricula.getIdMatricula())
                .anyMatch(otra -> otra.getIdCurso() == curso.getIdCurso());
        if (otroGrupoDelMismoCurso) {
            throw ReglaVioladaException.deConflicto("El estudiante ya cursa «" + curso.getNombre() + "» en "
                    + periodo.getNombre() + ": elija un curso distinto o cambie de periodo.");
        }
    }

    /** El cupo no se puede pasar: se cuenta primero cuántos están hoy en la sección. */
    private void asegurarCupoDisponible(Seccion seccion, Matricula matricula) {
        // Contar y guardar son dos pasos: si dos peticiones llegan juntas, un duplicado del mismo
        // estudiante lo frena el índice único de la base, pero el último cupo puede quedarse corto.
        int ocupados = matriculas.contarPorSeccion(seccion.getIdSeccion());
        if (ocupados >= seccion.getCupoMaximo()) {
            throw ReglaVioladaException.deConflicto("La sección " + seccion.describir() + " está llena ("
                    + ocupados + "/" + seccion.getCupoMaximo() + " cupos).");
        }
    }

    /** Nadie acumula más créditos de lo permitido en un mismo periodo. */
    private void asegurarTopeDeCreditos(Estudiante estudiante, Curso curso, PeriodoAcademico periodo,
            Matricula matricula) {
        int yaCursando = matriculas.listarPorEstudianteYPeriodo(estudiante.getIdEstudiante(), periodo.getNombre())
                .stream()
                .filter(otra -> otra.getIdMatricula() != matricula.getIdMatricula())
                .mapToInt(otra -> creditosDe(otra.getIdCurso()))
                .sum();
        if (yaCursando + curso.getCreditos() > ReglasAcademicas.CREDITOS_MAXIMOS_POR_PERIODO) {
            throw ReglaVioladaException.deConflicto("El estudiante llegaría a " + (yaCursando + curso.getCreditos())
                    + " créditos en " + periodo.getNombre() + " y el máximo por periodo es de "
                    + ReglasAcademicas.CREDITOS_MAXIMOS_POR_PERIODO + ".");
        }
    }

    private int creditosDe(int idCurso) {
        Curso curso = cursos.leerPorId(idCurso);
        return curso == null ? 0 : curso.getCreditos();
    }
}
