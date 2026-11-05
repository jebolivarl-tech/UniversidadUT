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
 * Reglas para abrir, modificar y borrar grupos. Un grupo pertenece a un curso y a un periodo; dos
 * grupos del mismo curso no pueden usar la misma aula en el mismo periodo; el cupo no puede bajar
 * de la cantidad de gente que ya matriculó; y ni el grupo ni el curso se borran mientras haya
 * matrículas encima. Las cuentas y listados que necesitan estas reglas los traen los DAO, las
 * clases que consultan MySQL con JDBC y SQL.
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

    /** Antes de guardar, exige que el curso, el profesor y el periodo existan y revisa aula y cupo. */
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

    /** No se borra un curso con matrículas: la base las borraría en cascada junto con los grupos. */
    public void validarEliminacionDeCurso(int idCurso) {
        int matriculadas = matriculas.listarPorCurso(idCurso).size();
        if (matriculadas > 0) {
            Curso curso = Referencias.curso(cursos, idCurso);
            throw ReglaVioladaException.deConflicto("El curso «" + curso.getNombre() + "» tiene " + matriculadas
                    + " matrícula(s) en sus grupos: elimine primero las matrículas.");
        }
    }

    /**
     * Grupos que el estudiante puede elegir. La consulta SQL del DAO ya deja fuera los de otra
     * carrera, los de un periodo cerrado, los cursos que él ya cursa en ese periodo y los grupos
     * llenos; aquí se caen además los que lo pasarían del tope de créditos, para que el formulario
     * no ofrezca una inscripción que el servidor tendría que rechazar después.
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

    /** Dos grupos del mismo curso no comparten aula en el mismo periodo: la segunda se rechaza. */
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

    /** El cupo máximo no puede bajar de los estudiantes que ya tienen matrícula en esta sección. */
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
