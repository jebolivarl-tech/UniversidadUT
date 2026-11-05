package universidad.web;

import java.time.LocalDate;
import java.util.Set;
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;
import universidad.modelo.Carrera;
import universidad.modelo.Cuenta;
import universidad.modelo.Curso;
import universidad.modelo.Estudiante;
import universidad.modelo.Facultad;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.modelo.Seccion;

/**
 * Última revisión de lo que llega de la API REST antes de tocar la base de datos. El JSON del cuerpo de
 * la petición HTTP ya se convirtió en objeto; aquí se mira que los campos traigan algo, que quepan en la
 * columna de MySQL y que los valores posibles sean los mismos que la base de datos acepta. Copiar esos
 * límites a propósito permite contestar un 400 con un texto claro en lugar de dejar que la base de datos
 * falle y que {@link CrudController} tenga que adivinar el motivo.
 * Cada método devuelve un sí o un no; quien llama arma el mensaje.
 */
public final class Validaciones {

    /** Un correo con forma mínima: algo, arroba, algo con un punto. Sin espacios. */
    private static final String PATRON_EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    /** Usuario de 3 a 50 caracteres, solo letras, números, punto, guion bajo y guion: es lo que cabe y lo que se puede escribir a mano. */
    private static final String PATRON_USUARIO = "^[A-Za-z0-9._-]{3,50}$";

    private static final Set<String> ROLES = Set.of(Sesion.ADMINISTRATIVO, Sesion.PROFESOR, Sesion.ESTUDIANTE);
    private static final Set<String> ESTADOS_PERIODO = Set.of(
            PeriodoAcademico.ACTIVO, PeriodoAcademico.INACTIVO, PeriodoAcademico.CERRADO);
    private static final Set<String> ESTADOS_ASISTENCIA = Set.of(
            Asistencia.ASISTIO, Asistencia.FALTO, Asistencia.TARDANZA, Asistencia.JUSTIFICADO);

    /** Escala institucional de notas: de 0.0 a 5.0. */
    public static final double NOTA_MINIMA = 0.0;
    public static final double NOTA_MAXIMA = 5.0;

    private Validaciones() {
    }

    /** Nombre y apellidos, correo válido, fecha de nacimiento que no esté en el futuro y una carrera elegida. */
    public static boolean esEstudianteValido(Estudiante e) {
        return e != null
                && textoValido(e.getNombre(), 60)
                && textoValido(e.getApellido(), 60)
                && e.getEmail() != null && e.getEmail().length() <= 100 && e.getEmail().matches(PATRON_EMAIL)
                && e.getFechaNacimiento() != null
                && !e.getFechaNacimiento().toLocalDate().isAfter(LocalDate.now())
                && e.getIdCarrera() > 0;
    }

    /** Carrera con nombre y facultad apuntada. */
    public static boolean esCarreraValida(Carrera c) {
        return c != null && textoValido(c.getNombre(), 100) && c.getIdFacultad() > 0;
    }

    /** Facultad con nombre; el decano es opcional, pero si viene no puede pasar de 100 caracteres. */
    public static boolean esFacultadValida(Facultad f) {
        return f != null && textoValido(f.getNombre(), 100) && (f.getDecano() == null || f.getDecano().length() <= 100);
    }

    /** Profesor con nombre, apellidos y facultad; el correo puede ir vacío, pero si viene tiene que ser válido. */
    public static boolean esProfesorValido(Profesor p) {
        return p != null
                && textoValido(p.getNombre(), 60)
                && textoValido(p.getApellido(), 60)
                && (esVacio(p.getEmail()) || (p.getEmail().length() <= 100 && p.getEmail().matches(PATRON_EMAIL)))
                && p.getIdFacultad() > 0;
    }

    /** Periodo con nombre, fechas en orden (el inicio no puede caer después del fin) y un estado conocido. */
    public static boolean esPeriodoValido(PeriodoAcademico p) {
        return p != null
                && textoValido(p.getNombre(), 50)
                && p.getFechaInicio() != null
                && p.getFechaFin() != null
                && !p.getFechaInicio().after(p.getFechaFin())
                && p.getEstado() != null
                && ESTADOS_PERIODO.contains(p.getEstado().toUpperCase());
    }

    /** Curso con nombre, créditos entre 1 y 10, carrera y profesor asignados. */
    public static boolean esCursoValido(Curso c) {
        return c != null
                && textoValido(c.getNombre(), 100)
                && c.getCreditos() >= 1 && c.getCreditos() <= 10
                && c.getIdCarrera() > 0
                && c.getIdProfesor() > 0;
    }

    /** Sección con curso, profesor y periodo, aula opcional y un cupo entre 1 y 200. */
    public static boolean esSeccionValida(Seccion s) {
        return s != null
                && s.getIdCurso() > 0
                && s.getIdProfesor() > 0
                && s.getIdPeriodo() > 0
                && (s.getAula() == null || s.getAula().length() <= 50)
                && s.getCupoMaximo() >= 1 && s.getCupoMaximo() <= 200;
    }

    /**
     * Matrícula con estudiante y sección apuntados. El curso y el periodo no se comprueban aquí: los
     * deriva la sección en {@link universidad.servicio.ServicioMatriculas}, para que nadie pueda escribir
     * un periodo que no corresponde al grupo.
     */
    public static boolean esMatriculaValida(Matricula m) {
        return m != null
                && m.getIdEstudiante() > 0
                && m.getIdSeccion() > 0
                && notaOpcionalValida(m.getNota());
    }

    /** Asistencia de un estudiante en una sección, con fecha de hoy o anterior y un estado conocido. */
    public static boolean esAsistenciaValida(Asistencia a) {
        return a != null
                && a.getIdEstudiante() > 0
                && a.getIdSeccion() > 0
                && a.getFecha() != null
                && !a.getFecha().toLocalDate().isAfter(LocalDate.now())
                && a.getEstado() != null
                && ESTADOS_ASISTENCIA.contains(a.getEstado().toUpperCase());
    }

    /** Nota de un estudiante en una sección: cada parcial que venga debe estar en la escala, y al menos uno tiene que traer valor. */
    public static boolean esCalificacionValida(Calificacion c) {
        return c != null
                && c.getIdEstudiante() > 0
                && c.getIdSeccion() > 0
                && notaOpcionalValida(c.getNotaParcial1())
                && notaOpcionalValida(c.getNotaParcial2())
                && notaOpcionalValida(c.getNotaExamenFinal())
                && (c.getNotaParcial1() != null || c.getNotaParcial2() != null || c.getNotaExamenFinal() != null);
    }

    /**
     * Cuenta de acceso. Al crear, la contraseña es obligatoria y de diez caracteres como mínimo; al editar
     * puede simplemente no venir (campo ausente en el JSON), y eso significa "no la cambies". El usuario
     * tiene que encajar en el patrón, el rol tiene que ser uno de los tres conocidos, y si el rol es
     * PROFESOR o ESTUDIANTE la cuenta debe ir atada a su ficha: sin esa atadura no habría forma de saber
     * qué datos le tocan.
     */
    public static boolean esCuentaValida(Cuenta c, boolean esCreacion) {
        if (c == null || c.getUsername() == null || !c.getUsername().matches(PATRON_USUARIO)) {
            return false;
        }
        if (c.getRol() == null || !ROLES.contains(c.getRol().toUpperCase())) {
            return false;
        }
        if ((c.getIdEstudiante() != null && c.getIdEstudiante() < 1)
                || (c.getIdProfesor() != null && c.getIdProfesor() < 1)) {
            return false;
        }
        if (Sesion.ESTUDIANTE.equalsIgnoreCase(c.getRol()) && c.getIdEstudiante() == null) {
            return false;
        }
        if (Sesion.PROFESOR.equalsIgnoreCase(c.getRol()) && c.getIdProfesor() == null) {
            return false;
        }
        if (c.getPassword() == null) {
            return !esCreacion;
        }
        return c.getPassword().length() >= 10 && c.getPassword().length() <= 200;
    }

    /** Nota sin escribir es válido (aún no se toma); si viene, tiene que estar entre 0.0 y 5.0. */
    private static boolean notaOpcionalValida(Double nota) {
        return nota == null || (nota >= NOTA_MINIMA && nota <= NOTA_MAXIMA);
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Texto con algo dentro y sin pasarse del largo de la columna. */
    private static boolean textoValido(String valor, int maximo) {
        return valor != null && !valor.isBlank() && valor.length() <= maximo;
    }
}
