package universidad.servicio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import universidad.dao.CursoDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.ProfesorDAO;
import universidad.dao.SeccionDAO;
import universidad.modelo.Curso;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Seccion;

/**
 * Reglas de apertura de grupos y del curso que los agrupa: un grupo pertenece a un curso y a un
 * periodo, comparte aula solo si nadie la tiene en ese mismo curso y periodo, su cupo no puede
 * quedar por debajo de la gente que ya matriculó, y ninguno de los dos se borra mientras haya
 * matrículas encima.
 */
public class ServicioSecciones {

    private final SeccionDAO secciones;
    private final CursoDAO cursos;
    private final ProfesorDAO profesores;
    private final PeriodoAcademicoDAO periodos;
    private final MatriculaDAO matriculas;
    private final EstudianteDAO estudiantes;

    public ServicioSecciones(SeccionDAO secciones, CursoDAO cursos, ProfesorDAO profesores,
            PeriodoAcademicoDAO periodos, MatriculaDAO matriculas, EstudianteDAO estudiantes) {
        this.secciones = secciones;
        this.cursos = cursos;
        this.profesores = profesores;
        this.periodos = periodos;
        this.matriculas = matriculas;
        this.estudiantes = estudiantes;
    }

    /** Valida la escritura y deja la sección lista para guardar. */
    public void preparar(Seccion seccion) {
        Referencias.curso(cursos, seccion.getIdCurso());
        Referencias.profesor(profesores, seccion.getIdProfesor());
        Referencias.periodo(periodos, seccion.getIdPeriodo());
        asegurarAulaLibre(seccion);
        asegurarCupoCoherente(seccion);
    }

    /** Una sección con estudiantes matriculados no se borra: primero se retiran las matrículas. */
    public void validarEliminacion(int idSeccion) {
        int ocupados = matriculas.contarPorSeccion(idSeccion);
        if (ocupados > 0) {
            Seccion seccion = Referencias.seccion(secciones, idSeccion);
            throw ReglaVioladaException.deConflicto("La sección " + seccion.describir() + " tiene " + ocupados
                    + " estudiante(s) matriculado(s): elimine primero las matrículas.");
        }
    }

    /**
     * Borrar el curso arrastraría en cascada sus grupos y las matrículas ya cargadas de los
     * estudiantes, así que mientras haya matrículas el curso no se elimina.
     */
    public void validarEliminacionDeCurso(int idCurso) {
        int matriculadas = matriculas.listarPorCurso(idCurso).size();
        if (matriculadas > 0) {
            Curso curso = Referencias.curso(cursos, idCurso);
            throw ReglaVioladaException.deConflicto("El curso «" + curso.getNombre() + "» tiene " + matriculadas
                    + " matrícula(s) en sus grupos: elimine primero las matrículas.");
        }
    }

    /**
     * Grupos que el estudiante puede elegir. La consulta ya deja fuera los de otra carrera, los de un
     * periodo cerrado, los cursos que él ya cursa en ese periodo y los grupos llenos; aquí se caen
     * además los que lo pasarían del tope de créditos, para que el formulario no ofrezca una
     * inscripción que el servidor tendría que rechazar.
     */
    public List<Seccion> inscribiblesPara(int idEstudiante) {
        Referencias.estudiante(estudiantes, idEstudiante);
        Map<String, Integer> creditosPorPeriodo = new HashMap<>();
        for (Matricula propia : matriculas.listarPorEstudiante(idEstudiante)) {
            creditosPorPeriodo.merge(propia.getPeriodo(), creditosDe(propia.getIdCurso()), Integer::sum);
        }
        return secciones.listarInscribiblesPara(idEstudiante).stream()
                .filter(s -> creditosPorPeriodo.getOrDefault(nombreDelPeriodo(s), 0)
                        + creditosDe(s.getIdCurso()) <= ReglasAcademicas.CREDITOS_MAXIMOS_POR_PERIODO)
                .toList();
    }

    private String nombreDelPeriodo(Seccion seccion) {
        if (seccion.getNombrePeriodo() != null && !seccion.getNombrePeriodo().isBlank()) {
            return seccion.getNombrePeriodo();
        }
        PeriodoAcademico periodo = periodos.leerPorId(seccion.getIdPeriodo());
        return periodo == null ? "" : periodo.getNombre();
    }

    private int creditosDe(int idCurso) {
        Curso curso = cursos.leerPorId(idCurso);
        return curso == null ? 0 : curso.getCreditos();
    }

    private void asegurarAulaLibre(Seccion seccion) {
        String aula = seccion.getAula();
        if (aula == null || aula.isBlank()) {
            return;
        }
        // Comparación sin distinción de mayúsculas: así se comporta el índice único de la base.
        boolean ocupada = secciones.listarPorPeriodo(seccion.getIdPeriodo()).stream().anyMatch(otra ->
                otra.getIdSeccion() != seccion.getIdSeccion()
                        && otra.getIdCurso() == seccion.getIdCurso()
                        && aula.equalsIgnoreCase(otra.getAula()));
        if (ocupada) {
            throw ReglaVioladaException.deConflicto("Ya existe una sección del mismo curso con el aula "
                    + aula + " en este periodo.");
        }
    }

    private void asegurarCupoCoherente(Seccion seccion) {
        if (seccion.getIdSeccion() == 0) {
            return; // alta: nadie puede estar matriculado todavía
        }
        int ocupados = matriculas.contarPorSeccion(seccion.getIdSeccion());
        if (seccion.getCupoMaximo() < ocupados) {
            throw ReglaVioladaException.deConflicto("La sección ya tiene " + ocupados
                    + " matriculado(s): el cupo máximo no puede bajar de esa cifra.");
        }
    }
}
