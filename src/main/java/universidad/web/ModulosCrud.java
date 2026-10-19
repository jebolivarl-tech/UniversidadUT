package universidad.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import universidad.dao.AsistenciaDAO;
import universidad.dao.CalificacionDAO;
import universidad.dao.CarreraDAO;
import universidad.dao.CursoDAO;
import universidad.dao.EstudianteDAO;
import universidad.dao.FacultadDAO;
import universidad.dao.MatriculaDAO;
import universidad.dao.PeriodoAcademicoDAO;
import universidad.dao.ProfesorDAO;
import universidad.dao.SeccionDAO;
import universidad.dao.CarreraDAOImpl;
import universidad.dao.AsistenciaDAOImpl;
import universidad.dao.CalificacionDAOImpl;
import universidad.dao.CursoDAOImpl;
import universidad.dao.EstudianteDAOImpl;
import universidad.dao.FacultadDAOImpl;
import universidad.dao.MatriculaDAOImpl;
import universidad.dao.PeriodoAcademicoDAOImpl;
import universidad.dao.ProfesorDAOImpl;
import universidad.dao.SeccionDAOImpl;
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;
import universidad.modelo.Curso;
import universidad.modelo.Facultad;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.modelo.Seccion;

/**
 * Define y monta los CRUD REST de los módulos académicos sobre {@link CrudController}: filtros por
 * query string, roles por operación y las reglas propias de cada entidad (escala de notas, propiedad
 * de la sección del profesor, duplicados de asistencia).
 */
public final class ModulosCrud {

    private final FacultadDAO facultades = new FacultadDAOImpl();
    private final ProfesorDAO profesores = new ProfesorDAOImpl();
    private final PeriodoAcademicoDAO periodos = new PeriodoAcademicoDAOImpl();
    private final CursoDAO cursos = new CursoDAOImpl();
    private final SeccionDAO secciones = new SeccionDAOImpl();
    private final MatriculaDAO matriculas = new MatriculaDAOImpl();
    private final AsistenciaDAO asistencias = new AsistenciaDAOImpl();
    private final CalificacionDAO calificaciones = new CalificacionDAOImpl();
    private final EstudianteDAO estudiantes = new EstudianteDAOImpl();
    private final CarreraDAO carreras = new CarreraDAOImpl();

    public void registrar(Javalin app) {
        registrarFacultades(app);
        registrarProfesores(app);
        registrarPeriodos(app);
        registrarCursos(app);
        registrarSecciones(app);
        registrarMatriculas(app);
        registrarAsistencias(app);
        registrarCalificaciones(app);
    }

    public MiController controladorMiInformacion() {
        return new MiController(facultades, profesores, periodos, cursos, secciones, matriculas,
                asistencias, calificaciones, estudiantes, carreras);
    }

    private void registrarFacultades(Javalin app) {
        new CrudController<>(facultades, Facultad.class, "facultades", "la facultad")
                .fijarId(Facultad::setIdFacultad)
                .valido(Validaciones::esFacultadValida)
                .mensajeConflicto("La facultad indicada no existe o entra en conflicto con otra.")
                .registrar(app);
    }

    private void registrarProfesores(Javalin app) {
        new CrudController<>(profesores, Profesor.class, "profesores", "el profesor")
                .fijarId(Profesor::setIdProfesor)
                .antesEscribir((ctx, p) -> requerir(facultades.leerPorId(p.getIdFacultad()) != null, "la facultad"))
                .listado(ctx -> porFiltro(profesores.listarTodos(), Profesor::getIdFacultad, Http.enteroQuery(ctx, "facultad")))
                .valido(Validaciones::esProfesorValido)
                .mensajeConflicto("El correo ya está registrado o la facultad no existe.")
                .registrar(app);
    }

    private void registrarPeriodos(Javalin app) {
        new CrudController<>(periodos, PeriodoAcademico.class, "periodos", "el periodo académico")
                .fijarId(PeriodoAcademico::setIdPeriodo)
                .listado(ctx -> {
                    String estado = ctx.queryParam("estado");
                    return estado == null || estado.isBlank()
                            ? periodos.listarTodos()
                            : periodos.listarPorEstado(estado.trim().toUpperCase());
                })
                .valido(Validaciones::esPeriodoValido)
                .antesEscribir((ctx, p) -> p.setEstado(p.getEstado().toUpperCase()))
                .registrar(app);
    }

    private void registrarCursos(Javalin app) {
        new CrudController<>(cursos, Curso.class, "cursos", "el curso")
                .fijarId(Curso::setIdCurso)
                .antesEscribir((ctx, c) -> {
                    requerir(carreras.leerPorId(c.getIdCarrera()) != null, "la carrera");
                    requerir(profesores.leerPorId(c.getIdProfesor()) != null, "el profesor");
                })
                .listado(ctx -> {
                    List<Curso> porCarrera = porFiltro(cursos.listarTodos(), Curso::getIdCarrera, Http.enteroQuery(ctx, "carrera"));
                    return porFiltro(porCarrera, Curso::getIdProfesor, Http.enteroQuery(ctx, "profesor"));
                })
                .valido(Validaciones::esCursoValido)
                .mensajeConflicto("La carrera o el profesor del curso no existe.")
                .registrar(app);
    }

    private void registrarSecciones(Javalin app) {
        new CrudController<>(secciones, Seccion.class, "secciones", "la sección")
                .fijarId(Seccion::setIdSeccion)
                .antesEscribir((ctx, s2) -> {
                    requerir(cursos.leerPorId(s2.getIdCurso()) != null, "el curso");
                    requerir(profesores.leerPorId(s2.getIdProfesor()) != null, "el profesor");
                    requerir(periodos.leerPorId(s2.getIdPeriodo()) != null, "el periodo");
                    asegurarAulaLibre(s2);
                })
                .listado(ctx -> {
                    List<Seccion> resultado = Sesion.PROFESOR.equals(Sesion.rol(ctx))
                            ? secciones.listarPorProfesor(Sesion.requerirIdProfesor(ctx))
                            : secciones.listarTodos();
                    resultado = porFiltro(resultado, Seccion::getIdPeriodo, Http.enteroQuery(ctx, "periodo"));
                    resultado = porFiltro(resultado, Seccion::getIdCurso, Http.enteroQuery(ctx, "curso"));
                    return porFiltro(resultado, Seccion::getIdProfesor, Http.enteroQuery(ctx, "profesor"));
                })
                .valido(Validaciones::esSeccionValida)
                .mensajeConflicto("El curso, el profesor o el periodo de la sección no existe.")
                .registrar(app);
    }

    private void registrarMatriculas(Javalin app) {
        new CrudController<>(matriculas, Matricula.class, "matriculas", "la matrícula")
                .fijarId(Matricula::setIdMatricula)
                .antesEscribir((ctx, m) -> {
                    requerir(estudiantes.leerPorId(m.getIdEstudiante()) != null, "el estudiante");
                    requerir(cursos.leerPorId(m.getIdCurso()) != null, "el curso");
                })
                .listado(ctx -> {
                    List<Matricula> todas = matriculas.listarTodos();
                    if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
                        List<Integer> cursosPropios = secciones.listarPorProfesor(Sesion.requerirIdProfesor(ctx))
                                .stream().map(Seccion::getIdCurso).distinct().toList();
                        todas = todas.stream().filter(m -> cursosPropios.contains(m.getIdCurso())).toList();
                    }
                    List<Matricula> porEstudiante = porFiltro(todas,
                            Matricula::getIdEstudiante, Http.enteroQuery(ctx, "estudiante"));
                    return porFiltro(porEstudiante, Matricula::getIdCurso, Http.enteroQuery(ctx, "curso"));
                })
                .valido(Validaciones::esMatriculaValida)
                .mensajeConflicto("El estudiante o el curso no existe, o la matrícula ya está registrada.")
                .registrar(app);
    }

    private void registrarAsistencias(Javalin app) {
        new CrudController<>(asistencias, Asistencia.class, "asistencias", "la asistencia")
                .fijarId(Asistencia::setIdAsistencia)
                .listado(this::listarAsistenciasVisibles)
                .valido(Validaciones::esAsistenciaValida)
                .rolesEscritura(Sesion.ADMINISTRATIVO, Sesion.PROFESOR)
                .antesEscribir(this::validarAsistenciaEscribible)
                .mensajeConflicto("Ese estudiante ya tiene un registro de asistencia en la fecha indicada.")
                .registrar(app);
    }

    private void registrarCalificaciones(Javalin app) {
        new CrudController<>(calificaciones, Calificacion.class, "calificaciones", "la calificación")
                .fijarId(Calificacion::setIdCalificacion)
                .listado(this::listarCalificacionesVisibles)
                .valido(Validaciones::esCalificacionValida)
                .rolesEscritura(Sesion.ADMINISTRATIVO, Sesion.PROFESOR)
                .antesEscribir(this::validarCalificacionEscribible)
                .despuesEscribir(ModulosCrud::conPromedioCalculado)
                .mensajeConflicto("Ya existe una calificación para ese estudiante en esa sección. Actualícela.")
                .registrar(app);
    }

    /** El profesor solo ve asistencias de sus secciones; el administrativo ve todas. */
    private List<Asistencia> listarAsistenciasVisibles(Context ctx) {
        Integer seccion = Http.enteroQuery(ctx, "seccion");
        if (seccion != null) {
            asegurarSeccionVisible(ctx, seccion);
            Date fecha = Http.fechaQuery(ctx, "fecha");
            return fecha == null
                    ? asistencias.listarPorSeccion(seccion)
                    : asistencias.listarPorSeccionYFecha(seccion, fecha);
        }
        if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
            List<Asistencia> todas = asistencias.listarTodos();
            return todas.stream().filter(a -> esSeccionPropia(ctx, a.getIdSeccion())).toList();
        }
        return asistencias.listarTodos();
    }

    private List<Calificacion> listarCalificacionesVisibles(Context ctx) {
        Integer seccion = Http.enteroQuery(ctx, "seccion");
        if (seccion != null) {
            asegurarSeccionVisible(ctx, seccion);
            return calificaciones.listarPorSeccion(seccion);
        }
        Integer estudiante = Http.enteroQuery(ctx, "estudiante");
        if (estudiante != null) {
            return calificaciones.listarPorEstudiante(estudiante);
        }
        if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
            return calificaciones.listarTodos().stream()
                    .filter(c -> esSeccionPropia(ctx, c.getIdSeccion())).toList();
        }
        return calificaciones.listarTodos();
    }

    private void validarAsistenciaEscribible(Context ctx, Asistencia a) {
        a.setEstado(a.getEstado().toUpperCase());
        requerir(estudiantes.leerPorId(a.getIdEstudiante()) != null, "el estudiante");
        requerir(secciones.leerPorId(a.getIdSeccion()) != null, "la sección");
        asegurarSeccionEscribible(ctx, a.getIdSeccion());
        boolean repetida = asistencias.listarPorSeccionYFecha(a.getIdSeccion(), a.getFecha()).stream()
                .anyMatch(otra -> otra.getIdEstudiante() == a.getIdEstudiante()
                        && otra.getIdAsistencia() != a.getIdAsistencia());
        if (repetida) {
            throw new ApiException(409, "Ese estudiante ya tiene un registro de asistencia en la fecha indicada.");
        }
    }

    private void validarCalificacionEscribible(Context ctx, Calificacion c) {
        requerir(estudiantes.leerPorId(c.getIdEstudiante()) != null, "el estudiante");
        requerir(secciones.leerPorId(c.getIdSeccion()) != null, "la sección");
        asegurarSeccionEscribible(ctx, c.getIdSeccion());
    }

    /** Promedio y estado calculados igual que en la base de datos, para responder sin otra consulta. */
    private static Calificacion conPromedioCalculado(Calificacion c) {
        double promedio = Math.round(Calificacion.promedio(c.getNotaParcial1(), c.getNotaParcial2(), c.getNotaExamenFinal()) * 100) / 100.0;
        c.setPromedioFinal(promedio);
        c.setEstado(Calificacion.estado(c.getNotaParcial1(), c.getNotaParcial2(), c.getNotaExamenFinal()));
        return c;
    }

    private void asegurarSeccionVisible(Context ctx, int idSeccion) {
        if (Sesion.PROFESOR.equals(Sesion.rol(ctx)) && !esSeccionPropia(ctx, idSeccion)) {
            throw new ApiException(403, "Solo puede consultar las secciones que usted dicta.");
        }
    }

    private void asegurarSeccionEscribible(Context ctx, int idSeccion) {
        if (Sesion.ADMINISTRATIVO.equals(Sesion.rol(ctx))) {
            return;
        }
        if (!esSeccionPropia(ctx, idSeccion)) {
            throw new ApiException(403, "Solo puede registrar datos de las secciones que usted dicta.");
        }
    }

    private boolean esSeccionPropia(Context ctx, int idSeccion) {
        Integer mio = Sesion.idProfesor(ctx);
        if (mio == null) {
            return false;
        }
        Seccion seccion = secciones.leerPorId(idSeccion);
        return seccion != null && seccion.getIdProfesor() == mio;
    }

    /** La tabla secciones no tiene clave única para (curso, periodo, aula): se controla en la aplicación. */
    private void asegurarAulaLibre(Seccion seccion) {
        boolean ocupada = secciones.listarTodos().stream().anyMatch(otra ->
                otra.getIdSeccion() != seccion.getIdSeccion()
                        && otra.getIdCurso() == seccion.getIdCurso()
                        && otra.getIdPeriodo() == seccion.getIdPeriodo()
                        && Objects.equals(otra.getAula(), seccion.getAula()));
        if (ocupada) {
            throw new ApiException(409, "Ya existe una sección con esa aula para el mismo curso y periodo.");
        }
    }

    private static void requerir(boolean existe, String nombreRecurso) {
        if (!existe) {
            throw new ApiException(400, "No se encontró " + nombreRecurso + " registrado en la base de datos.");
        }
    }

    private static <T> List<T> porFiltro(List<T> lista, Function<T, Integer> lector, Integer filtro) {
        if (filtro == null) {
            return lista;
        }
        return lista.stream().filter(item -> filtro.equals(lector.apply(item))).toList();
    }
}
