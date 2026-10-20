package universidad.servicio;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import universidad.dao.AsistenciaDAO;
import universidad.dao.CursoDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.ProfesorDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Asistencia;
import universidad.modelo.Curso;
import universidad.modelo.Estudiante;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.modelo.Seccion;

/**
 * Doble en memoria de la base de datos para probar las reglas sin MySQL: los DAOs son dobles de
 * Mockito y aquí están los constructores cortos de cada modelo. El escenario trae el camino feliz
 * (estudiante de la carrera 1, sección del curso 5, periodo ACTIVO con cupo) y cada prueba rompe
 * una sola regla.
 */
final class Escenario {

    static final int ESTUDIANTE = 7;
    static final int SECCION = 3;
    static final int CURSO = 5;
    static final int PERIODO = 2;
    static final int PROFESOR = 4;
    static final int CARRERA = 1;
    static final String PERIODO_NOMBRE = "2026-2";
    static final Date INICIO = Date.valueOf("2026-07-15");
    static final Date FIN = Date.valueOf("2026-12-15");
    static final Date HOY = Date.valueOf("2026-10-19");

    final EstudianteDAO estudiantes = mock(EstudianteDAO.class);
    final CursoDAO cursos = mock(CursoDAO.class);
    final SeccionDAO secciones = mock(SeccionDAO.class);
    final PeriodoAcademicoDAO periodos = mock(PeriodoAcademicoDAO.class);
    final MatriculaDAO matriculas = mock(MatriculaDAO.class);
    final AsistenciaDAO asistencias = mock(AsistenciaDAO.class);
    final ProfesorDAO profesores = mock(ProfesorDAO.class);

    final ServicioMatriculas matriculasServicio =
            new ServicioMatriculas(estudiantes, cursos, secciones, periodos, matriculas, asistencias);
    final ServicioSecciones seccionesServicio =
            new ServicioSecciones(secciones, cursos, profesores, periodos, matriculas, estudiantes);
    final ServicioAsistencias asistenciasServicio =
            new ServicioAsistencias(asistencias, estudiantes, secciones, matriculas, periodos);
    final ServicioCalificaciones calificacionesServicio =
            new ServicioCalificaciones(estudiantes, secciones, matriculas, asistencias, matriculasServicio);

    Escenario() {
        Estudiante alumno = new Estudiante("María", "Rojas", "maria@ut.edu.co", Date.valueOf("2003-04-10"), CARRERA);
        alumno.setIdEstudiante(ESTUDIANTE);
        when(estudiantes.leerPorId(ESTUDIANTE)).thenReturn(alumno);

        when(cursos.leerPorId(CURSO)).thenReturn(curso(CURSO, "Cálculo", 3, CARRERA));

        Seccion grupo = seccion(SECCION, "B-101", 30);
        when(secciones.leerPorId(SECCION)).thenReturn(grupo);
        when(secciones.listarPorPeriodo(PERIODO)).thenReturn(List.of(grupo));

        when(periodos.leerPorId(PERIODO)).thenReturn(periodo(PERIODO_NOMBRE, PeriodoAcademico.ACTIVO));

        Profesor docente = new Profesor("Jorge", "Cardozo", "jorge@ut.edu.co", 1);
        docente.setIdProfesor(PROFESOR);
        when(profesores.leerPorId(PROFESOR)).thenReturn(docente);
    }

    /** Sección del curso y periodo del escenario, con el aula y el cupo que pida la prueba. */
    Seccion seccion(int idSeccion, String aula, int cupo) {
        Seccion s = new Seccion(CURSO, PROFESOR, PERIODO, aula, cupo);
        s.setIdSeccion(idSeccion);
        s.setNombreCurso("Cálculo");
        s.setNombrePeriodo(PERIODO_NOMBRE);
        return s;
    }

    void seccionCon(String aula, int cupo) {
        when(secciones.leerPorId(SECCION)).thenReturn(seccion(SECCION, aula, cupo));
    }

    void periodoEn(String estado) {
        when(periodos.leerPorId(PERIODO)).thenReturn(periodo(PERIODO_NOMBRE, estado));
    }

    void cursoDeOtraCarrera() {
        when(cursos.leerPorId(CURSO)).thenReturn(curso(CURSO, "Cálculo", 3, CARRERA + 1));
    }

    Curso curso(int idCurso, String nombre, int creditos, int idCarrera) {
        Curso c = new Curso(nombre, creditos, idCarrera, PROFESOR);
        c.setIdCurso(idCurso);
        return c;
    }

    PeriodoAcademico periodo(String nombre, String estado) {
        PeriodoAcademico p = new PeriodoAcademico(nombre, INICIO, FIN, estado);
        p.setIdPeriodo(PERIODO);
        return p;
    }

    /** Inscripción nueva: sin id, todavía no está en la base. */
    Matricula inscripcion(Double nota) {
        Matricula m = new Matricula();
        m.setIdEstudiante(ESTUDIANTE);
        m.setIdSeccion(SECCION);
        m.setNota(nota);
        return m;
    }

    /** Matrícula ya guardada en la sección del escenario, lista para que el servicio la encuentre por id. */
    Matricula matriculaGuardada(int idMatricula, Double nota) {
        Matricula m = unaMatricula(idMatricula, nota);
        when(matriculas.leerPorId(idMatricula)).thenReturn(m);
        return m;
    }

    /** La misma matrícula pero sin avisar al doble: se usa cuando la devuelve otra stub. */
    Matricula unaMatricula(int idMatricula, Double nota) {
        Matricula m = inscripcion(nota);
        m.setIdMatricula(idMatricula);
        m.setIdCurso(CURSO);
        m.setPeriodo(PERIODO_NOMBRE);
        return m;
    }

    /** Cambiar de sección: la previa apunta a otro grupo. */
    Matricula matriculaEnOtraSeccion(int idMatricula, int idSeccionPrevias) {
        Matricula m = matriculaGuardada(idMatricula, null);
        m.setIdSeccion(idSeccionPrevias);
        return m;
    }

    /** El estudiante ya está matriculado en la sección del escenario (lo que exigen asistencia y notas). */
    void estaMatriculado() {
        when(matriculas.leerPorEstudianteYSeccion(ESTUDIANTE, SECCION))
                .thenReturn(unaMatricula(100, null));
    }

    void noEstaMatriculado() {
        when(matriculas.leerPorEstudianteYSeccion(ESTUDIANTE, SECCION)).thenReturn(null);
    }

    void ocupacionDeSeccion(int ocupados) {
        when(matriculas.contarPorSeccion(SECCION)).thenReturn(ocupados);
    }

    /** Matrículas ya cargadas en los grupos del curso del escenario: las que bloquean su borrado. */
    void matriculasEnCurso(int cantidad) {
        List<Matricula> cargadas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            cargadas.add(unaMatricula(300 + i, null));
        }
        when(matriculas.listarPorCurso(CURSO)).thenReturn(cargadas);
    }

    /** El estudiante ya cursa el curso del escenario en el periodo, pero en otro grupo. */
    void yaInscritoEnOtroGrupoDelCurso() {
        Matricula previa = unaMatricula(210, null);
        previa.setIdSeccion(9);
        when(matriculas.listarPorEstudianteYPeriodo(ESTUDIANTE, PERIODO_NOMBRE)).thenReturn(List.of(previa));
    }

    /** Grupos que la base devuelve como inscribibles para el estudiante del escenario. */
    void inscribibles(Seccion... grupos) {
        when(secciones.listarInscribiblesPara(ESTUDIANTE)).thenReturn(List.of(grupos));
    }

    /** Matrículas del estudiante en el periodo: las que suman contra el tope de los inscribibles. */
    void inscritasEnElPeriodo(int... creditos) {
        List<Matricula> previas = new ArrayList<>();
        for (int i = 0; i < creditos.length; i++) {
            int idCurso = 60 + i;
            when(cursos.leerPorId(idCurso)).thenReturn(curso(idCurso, "Curso " + idCurso, creditos[i], CARRERA));
            Matricula m = new Matricula();
            m.setIdMatricula(400 + i);
            m.setIdEstudiante(ESTUDIANTE);
            m.setIdCurso(idCurso);
            m.setPeriodo(PERIODO_NOMBRE);
            previas.add(m);
        }
        when(matriculas.listarPorEstudiante(ESTUDIANTE)).thenReturn(previas);
    }

    /** Otro grupo del mismo curso y periodo: es la que hace chocar la regla del aula. */
    void otraSeccionEnElPeriodo(int idSeccion, String aula) {
        List<Seccion> grupos = new ArrayList<>(secciones.listarPorPeriodo(PERIODO));
        grupos.add(seccion(idSeccion, aula, 30));
        when(secciones.listarPorPeriodo(PERIODO)).thenReturn(grupos);
    }

    /** Controles de asistencia del estudiante en la sección, en el orden en que se marcaron. */
    void controlesDeAsistencia(String... estados) {
        List<Asistencia> controles = new ArrayList<>();
        for (String estado : estados) {
            Asistencia a = new Asistencia(ESTUDIANTE, SECCION, HOY, estado);
            a.setIdAsistencia(controles.size() + 1);
            controles.add(a);
        }
        when(asistencias.listarPorEstudianteYSeccion(ESTUDIANTE, SECCION)).thenReturn(controles);
        when(asistencias.listarPorSeccionYFecha(SECCION, HOY)).thenReturn(controles);
    }

    /** Cursos ya inscrito en el periodo: cada uno suma sus créditos al tope. */
    void yaInscritoEnCursosDeCreditos(int... creditos) {
        List<Matricula> previas = new ArrayList<>();
        for (int i = 0; i < creditos.length; i++) {
            int idCurso = 50 + i;
            when(cursos.leerPorId(idCurso)).thenReturn(curso(idCurso, "Curso " + idCurso, creditos[i], CARRERA));
            Matricula m = new Matricula();
            m.setIdMatricula(200 + i);
            m.setIdEstudiante(ESTUDIANTE);
            m.setIdCurso(idCurso);
            previas.add(m);
        }
        when(matriculas.listarPorEstudianteYPeriodo(ESTUDIANTE, PERIODO_NOMBRE)).thenReturn(previas);
    }
}
