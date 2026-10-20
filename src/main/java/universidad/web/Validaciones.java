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

/** Reglas de validación de entrada (alineadas con los tamaños y enums de la base de datos). */
public final class Validaciones {

    private static final String PATRON_EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
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

    public static boolean esEstudianteValido(Estudiante e) {
        return e != null
                && textoValido(e.getNombre(), 60)
                && textoValido(e.getApellido(), 60)
                && e.getEmail() != null && e.getEmail().length() <= 100 && e.getEmail().matches(PATRON_EMAIL)
                && e.getFechaNacimiento() != null
                && !e.getFechaNacimiento().toLocalDate().isAfter(LocalDate.now())
                && e.getIdCarrera() > 0;
    }

    public static boolean esCarreraValida(Carrera c) {
        return c != null && textoValido(c.getNombre(), 100) && c.getIdFacultad() > 0;
    }

    public static boolean esFacultadValida(Facultad f) {
        return f != null && textoValido(f.getNombre(), 100) && (f.getDecano() == null || f.getDecano().length() <= 100);
    }

    public static boolean esProfesorValido(Profesor p) {
        return p != null
                && textoValido(p.getNombre(), 60)
                && textoValido(p.getApellido(), 60)
                && (esVacio(p.getEmail()) || (p.getEmail().length() <= 100 && p.getEmail().matches(PATRON_EMAIL)))
                && p.getIdFacultad() > 0;
    }

    public static boolean esPeriodoValido(PeriodoAcademico p) {
        return p != null
                && textoValido(p.getNombre(), 50)
                && p.getFechaInicio() != null
                && p.getFechaFin() != null
                && !p.getFechaInicio().after(p.getFechaFin())
                && p.getEstado() != null
                && ESTADOS_PERIODO.contains(p.getEstado().toUpperCase());
    }

    public static boolean esCursoValido(Curso c) {
        return c != null
                && textoValido(c.getNombre(), 100)
                && c.getCreditos() >= 1 && c.getCreditos() <= 10
                && c.getIdCarrera() > 0
                && c.getIdProfesor() > 0;
    }

    public static boolean esSeccionValida(Seccion s) {
        return s != null
                && s.getIdCurso() > 0
                && s.getIdProfesor() > 0
                && s.getIdPeriodo() > 0
                && (s.getAula() == null || s.getAula().length() <= 50)
                && s.getCupoMaximo() >= 1 && s.getCupoMaximo() <= 200;
    }

    /** El curso y el periodo no se validan aquí: los deriva la sección en ServicioMatriculas. */
    public static boolean esMatriculaValida(Matricula m) {
        return m != null
                && m.getIdEstudiante() > 0
                && m.getIdSeccion() > 0
                && notaOpcionalValida(m.getNota());
    }

    public static boolean esAsistenciaValida(Asistencia a) {
        return a != null
                && a.getIdEstudiante() > 0
                && a.getIdSeccion() > 0
                && a.getFecha() != null
                && !a.getFecha().toLocalDate().isAfter(LocalDate.now())
                && a.getEstado() != null
                && ESTADOS_ASISTENCIA.contains(a.getEstado().toUpperCase());
    }

    public static boolean esCalificacionValida(Calificacion c) {
        return c != null
                && c.getIdEstudiante() > 0
                && c.getIdSeccion() > 0
                && notaOpcionalValida(c.getNotaParcial1())
                && notaOpcionalValida(c.getNotaParcial2())
                && notaOpcionalValida(c.getNotaExamenFinal())
                && (c.getNotaParcial1() != null || c.getNotaParcial2() != null || c.getNotaExamenFinal() != null);
    }

    /** En creación la contraseña es obligatoria; en edición puede venir vacía (no se cambia). */
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

    private static boolean notaOpcionalValida(Double nota) {
        return nota == null || (nota >= NOTA_MINIMA && nota <= NOTA_MAXIMA);
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static boolean textoValido(String valor, int maximo) {
        return valor != null && !valor.isBlank() && valor.length() <= maximo;
    }
}
