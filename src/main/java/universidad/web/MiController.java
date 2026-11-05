package universidad.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;
import universidad.modelo.Carrera;
import universidad.modelo.Curso;
import universidad.modelo.Estudiante;
import universidad.modelo.Facultad;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.servicio.ReglasAcademicas;
import universidad.modelo.Seccion;

/**
 * Lo que cada usuario ve de lo suyo dentro de la API REST (las rutas HTTP que llama el navegador para
 * traer datos, en vez de páginas): /api/mi devuelve el perfil, las secciones, las calificaciones, las
 * asistencias y las matrículas de la cuenta que está en sesión. La sesión es el registro que quedó en
 * el servidor tras iniciar sesión, con el rol y las fichas vinculadas.
 *
 * Hace falta este controlador aparte porque el estudiante no tiene permiso para los listados generales
 * de cursos, profesores o periodos. Sería un callejón sin salida: recibiría números sueltos sin saber
 * a quién corresponden. Así que aquí el servidor, que sí puede leer esas tablas por su DAO (la clase
 * que escribe y lee cada tabla con JDBC, la librería de Java para hablar con MySQL), arma el nombre de
 * curso, profesor y periodo antes de responder.
 *
 * Lo importante de la seguridad: el id con el que se filtra se saca de la sesión, nunca de lo que el
 * navegador escriba en la dirección. Por eso un estudiante no puede ver las notas de otro cambiando un
 * número en la URL.
 */
public final class MiController {

    private final FacultadDAO facultades;
    private final ProfesorDAO profesores;
    private final PeriodoAcademicoDAO periodos;
    private final CursoDAO cursos;
    private final SeccionDAO secciones;
    private final MatriculaDAO matriculas;
    private final AsistenciaDAO asistencias;
    private final CalificacionDAO calificaciones;
    private final EstudianteDAO estudiantes;
    private final CarreraDAO carreras;

    public MiController(FacultadDAO facultades, ProfesorDAO profesores, PeriodoAcademicoDAO periodos,
                        CursoDAO cursos, SeccionDAO secciones, MatriculaDAO matriculas,
                        AsistenciaDAO asistencias, CalificacionDAO calificaciones,
                        EstudianteDAO estudiantes, CarreraDAO carreras) {
        this.facultades = facultades;
        this.profesores = profesores;
        this.periodos = periodos;
        this.cursos = cursos;
        this.secciones = secciones;
        this.matriculas = matriculas;
        this.asistencias = asistencias;
        this.calificaciones = calificaciones;
        this.estudiantes = estudiantes;
        this.carreras = carreras;
    }

    /** Monta las cinco rutas /api/mi en la aplicación. */
    public void registrar(Javalin app) {
        app.get("/api/mi", this::perfil);
        app.get("/api/mi/secciones", this::listarSecciones);
        app.get("/api/mi/calificaciones", this::listarCalificaciones);
        app.get("/api/mi/asistencias", this::listarAsistencias);
        app.get("/api/mi/matriculas", this::listarMatriculas);
    }

    /** Tarjeta de la persona en sesión: nombre, carrera o facultad y un resumen de sus notas. */
    private void perfil(Context ctx) {
        Sesion.requerirAutenticacion(ctx);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        // Lo que se responde sale de la sesión, no de un parámetro que alguien pudiera inventar.
        respuesta.put("username", Sesion.username(ctx));
        respuesta.put("rol", Sesion.rol(ctx));

        if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
            Profesor profesor = profesores.leerPorId(Sesion.requerirIdProfesor(ctx));
            respuesta.put("profesor", nombreCompleto(profesor));
            respuesta.put("facultad", profesor == null ? "" : nombreFacultad(profesor.getIdFacultad()));
            respuesta.put("secciones", profesor == null ? 0 : secciones.listarPorProfesor(profesor.getIdProfesor()).size());
            ctx.json(respuesta);
            return;
        }

        if (Sesion.ESTUDIANTE.equals(Sesion.rol(ctx))) {
            Estudiante estudiante = estudiantes.leerPorId(Sesion.requerirIdEstudiante(ctx));
            Carrera carrera = estudiante == null ? null : carreras.leerPorId(estudiante.getIdCarrera());
            // Listas completas convertidas a consulta por id: así armar el nombre de cada materia no
            // obliga a recorrer la lista de cursos una vez por nota.
            Map<Integer, Seccion> mapaSecciones = porId(secciones.listarTodos(), Seccion::getIdSeccion);
            Map<Integer, Curso> mapaCursos = porId(cursos.listarTodos(), Curso::getIdCurso);

            List<Calificacion> misNotas = estudiante == null
                    ? List.of() : calificaciones.listarPorEstudiante(estudiante.getIdEstudiante());
            List<Asistencia> misControles = estudiante == null
                    ? List.of() : asistencias.listarPorEstudiante(estudiante.getIdEstudiante());

            respuesta.put("estudiante", estudiante == null ? "" : estudiante.getNombre() + " " + estudiante.getApellido());
            respuesta.put("email", estudiante == null || estudiante.getEmail() == null ? "" : estudiante.getEmail());
            respuesta.put("carrera", carrera == null ? "" : carrera.getNombre());
            respuesta.put("facultad", carrera == null ? "" : nombreFacultad(carrera.getIdFacultad()));
            respuesta.put("materias", misNotas.stream()
                    .map(c -> nombre(cursoDe(c.getIdSeccion(), mapaSecciones, mapaCursos))).distinct().toList());
            respuesta.putAll(resumen(misNotas, misControles));
            ctx.json(respuesta);
            return;
        }

        // El administrativo se queda solo con usuario y rol: él ya ve todo en sus módulos.
        ctx.json(respuesta);
    }

    /** Grupos del que está en sesión: los del estudiante por su matrícula, los del profesor por su id. */
    private void listarSecciones(Context ctx) {
        Sesion.requerirAutenticacion(ctx);
        Map<Integer, Curso> mapaCursos = porId(cursos.listarTodos(), Curso::getIdCurso);
        Map<Integer, Profesor> mapaProfesores = porId(profesores.listarTodos(), Profesor::getIdProfesor);
        Map<Integer, PeriodoAcademico> mapaPeriodos = porId(periodos.listarTodos(), PeriodoAcademico::getIdPeriodo);

        List<Seccion> mias;
        if (Sesion.ESTUDIANTE.equals(Sesion.rol(ctx))) {
            mias = secciones.listarPorEstudiante(Sesion.requerirIdEstudiante(ctx));
        } else if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
            mias = secciones.listarPorProfesor(Sesion.requerirIdProfesor(ctx));
        } else {
            throw new ApiException(403, "Su rol no tiene secciones propias; use el módulo de secciones.");
        }

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Seccion s : mias) {
            Curso curso = mapaCursos.get(s.getIdCurso());
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idSeccion", s.getIdSeccion());
            fila.put("curso", nombre(curso, Curso::getNombre));
            fila.put("creditos", curso == null ? null : curso.getCreditos());
            fila.put("profesor", nombreCompleto(mapaProfesores.get(s.getIdProfesor())));
            fila.put("periodo", nombre(mapaPeriodos.get(s.getIdPeriodo()), PeriodoAcademico::getNombre));
            fila.put("aula", s.getAula());
            // El cupo se manda ya contado ("12 de 30") para que la página no tenga que sumarlo.
            fila.put("ocupacion", s.getCuposOcupados() + " de " + s.getCupoMaximo());
            filas.add(fila);
        }
        ctx.json(filas);
    }

    /** Notas del estudiante, o de los estudiantes de las secciones que dicta el profesor. */
    private void listarCalificaciones(Context ctx) {
        Sesion.requerirAutenticacion(ctx);
        Map<Integer, Curso> mapaCursos = porId(cursos.listarTodos(), Curso::getIdCurso);
        Map<Integer, Seccion> mapaSecciones = porId(secciones.listarTodos(), Seccion::getIdSeccion);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Calificacion c : misCalificaciones(ctx)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idCalificacion", c.getIdCalificacion());
            fila.put("idSeccion", c.getIdSeccion());
            fila.put("curso", nombre(cursoDe(c.getIdSeccion(), mapaSecciones, mapaCursos), Curso::getNombre));
            fila.put("notaParcial1", c.getNotaParcial1());
            fila.put("notaParcial2", c.getNotaParcial2());
            fila.put("notaExamenFinal", c.getNotaExamenFinal());
            fila.put("promedioFinal", c.getPromedioFinal());
            fila.put("estado", c.getEstado());
            filas.add(fila);
        }
        ctx.json(filas);
    }

    /** Controles de asistencia de lo mismo: las propias del estudiante, las de las secciones del profesor. */
    private void listarAsistencias(Context ctx) {
        Sesion.requerirAutenticacion(ctx);
        Map<Integer, Curso> mapaCursos = porId(cursos.listarTodos(), Curso::getIdCurso);
        Map<Integer, Seccion> mapaSecciones = porId(secciones.listarTodos(), Seccion::getIdSeccion);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Asistencia a : misAsistencias(ctx)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idAsistencia", a.getIdAsistencia());
            fila.put("fecha", a.getFecha() == null ? "" : a.getFecha().toString());
            fila.put("curso", nombre(cursoDe(a.getIdSeccion(), mapaSecciones, mapaCursos), Curso::getNombre));
            fila.put("estado", a.getEstado());
            filas.add(fila);
        }
        ctx.json(filas);
    }

    private void listarMatriculas(Context ctx) {
        Sesion.requerirAutenticacion(ctx);
        if (!Sesion.ESTUDIANTE.equals(Sesion.rol(ctx))) {
            throw new ApiException(403, "Las matrículas propias solo aplican al rol ESTUDIANTE.");
        }
        Map<Integer, Curso> mapaCursos = porId(cursos.listarTodos(), Curso::getIdCurso);

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Matricula m : matriculas.listarPorEstudiante(Sesion.requerirIdEstudiante(ctx))) {
            Curso curso = mapaCursos.get(m.getIdCurso());
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idMatricula", m.getIdMatricula());
            fila.put("idSeccion", m.getIdSeccion());
            fila.put("curso", nombre(curso, Curso::getNombre));
            fila.put("creditos", curso == null ? null : curso.getCreditos());
            fila.put("periodo", m.getPeriodo());
            fila.put("nota", m.getNota());
            fila.put("estado", m.getEstado());
            filas.add(fila);
        }
        ctx.json(filas);
    }

    /**
     * Decide qué notas se listan según el rol. El id del estudiante y del profesor salen de la sesión;
     * si el profesor pide todo, aquí mismo se descartan las secciones que no dicta, y el resto de roles
     * se manda al módulo general con un 403.
     */
    private List<Calificacion> misCalificaciones(Context ctx) {
        if (Sesion.ESTUDIANTE.equals(Sesion.rol(ctx))) {
            return calificaciones.listarPorEstudiante(Sesion.requerirIdEstudiante(ctx));
        }
        if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
            return calificaciones.listarTodos().stream()
                    .filter(c -> esSeccionPropia(ctx, c.getIdSeccion())).toList();
        }
        throw new ApiException(403, "Su rol no tiene calificaciones propias; use el módulo de calificaciones.");
    }

    /** Lo mismo para las asistencias, con el mismo filtro por secciones propias. */
    private List<Asistencia> misAsistencias(Context ctx) {
        if (Sesion.ESTUDIANTE.equals(Sesion.rol(ctx))) {
            return asistencias.listarPorEstudiante(Sesion.requerirIdEstudiante(ctx));
        }
        if (Sesion.PROFESOR.equals(Sesion.rol(ctx))) {
            return asistencias.listarTodos().stream()
                    .filter(a -> esSeccionPropia(ctx, a.getIdSeccion())).toList();
        }
        throw new ApiException(403, "Su rol no tiene asistencias propias; use el módulo de asistencias.");
    }

    /** Compara el profesor de la sección con el id del profesor en sesión; sin ficha o sin sección, no es suya. */
    private boolean esSeccionPropia(Context ctx, int idSeccion) {
        Integer mio = Sesion.idProfesor(ctx);
        Seccion seccion = secciones.leerPorId(idSeccion);
        return mio != null && seccion != null && seccion.getIdProfesor() == mio;
    }

    /** De una sección se saca el curso que dicta; null si la sección ya no existe. */
    private Curso cursoDe(int idSeccion, Map<Integer, Seccion> mapaSecciones, Map<Integer, Curso> mapaCursos) {
        Seccion seccion = mapaSecciones.get(idSeccion);
        return seccion == null ? null : mapaCursos.get(seccion.getIdCurso());
    }

    /** Promedio general, materias aprobadas, perdidas por inasistencia y porcentaje de asistencia. Solo cuenta las notas ya cerradas. */
    private static Map<String, Object> resumen(List<Calificacion> notas, List<Asistencia> controles) {
        List<Double> definitivas = notas.stream()
                .filter(c -> !Calificacion.EN_CURSO.equals(c.getEstado()))
                .map(Calificacion::getPromedioFinal)
                .filter(Objects::nonNull)
                .toList();
        long aprobadas = notas.stream().filter(c -> Calificacion.APROBADO.equals(c.getEstado())).count();
        long perdidasPorAsistencia = notas.stream()
                .filter(c -> Calificacion.PERDIDO_POR_FALTA_DE_ASISTENCIA.equals(c.getEstado())).count();

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("materiasCalificadas", definitivas.size());
        resumen.put("materiasAprobadas", aprobadas);
        resumen.put("materiasPerdidasPorAsistencia", perdidasPorAsistencia);
        resumen.put("promedioGeneral", definitivas.isEmpty() ? null
                : Math.round(definitivas.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) / 100.0);
        resumen.put("controles", controles.size());
        resumen.put("porcentajeAsistencia", controles.isEmpty() ? null
                : Math.round(ReglasAcademicas.porcentajeDeAsistencia(controles) * 10000) / 100.0);
        return resumen;
    }

    private String nombreFacultad(int idFacultad) {
        Facultad facultad = facultades.leerPorId(idFacultad);
        return facultad == null ? "" : facultad.getNombre();
    }

    // Los métodos de abajo devuelven cadena vacía en lugar de fallar: una ficha borrada o sin vincular
    // no debe tumbar la respuesta de toda la página.

    private static String nombreCompleto(Profesor p) {
        return p == null ? "" : p.getNombre() + " " + p.getApellido();
    }

    private static String nombre(Curso curso) {
        return curso == null ? "" : curso.getNombre();
    }

    /** El nombre del campo que se pida del objeto, o cadena vacía si el objeto no está. */
    private static <T> String nombre(T item, Function<T, String> lector) {
        return item == null ? "" : lector.apply(item);
    }

    /** Pasa una lista a consulta por id, para no recorrerla entera cada vez que hay que buscar un nombre. */
    private static <T> Map<Integer, T> porId(List<T> lista, Function<T, Integer> lectorId) {
        return lista.stream().collect(Collectors.toMap(lectorId, item -> item, (a, b) -> a));
    }
}
