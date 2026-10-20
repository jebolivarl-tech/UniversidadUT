package universidad.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;
import universidad.modelo.Cuenta;
import universidad.modelo.Curso;
import universidad.modelo.Facultad;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;
import universidad.modelo.Profesor;
import universidad.modelo.Seccion;

class ModulosValidacionesTest {

    private static final Date HOY = Date.valueOf(LocalDate.now());

    @Test
    void facultaes() {
        assertTrue(Validaciones.esFacultadValida(new Facultad("Ingeniería", null)));
        assertFalse(Validaciones.esFacultadValida(new Facultad(" ", "x")));
        assertFalse(Validaciones.esFacultadValida(new Facultad("n".repeat(101), null)));
    }

    @Test
    void profesores() {
        assertTrue(Validaciones.esProfesorValido(new Profesor("Ana", "Ruiz", "ana@ut.edu.co", 1)));
        assertFalse(Validaciones.esProfesorValido(new Profesor("Ana", "Ruiz", "ana@ut.edu.co", 0)));
        assertFalse(Validaciones.esProfesorValido(new Profesor("Ana", "Ruiz", "correo-malo", 1)));
    }

    @Test
    void periodos() {
        assertTrue(Validaciones.esPeriodoValido(
                new PeriodoAcademico("2026-1", Date.valueOf("2026-01-15"), Date.valueOf("2026-06-15"), "ACTIVO")));
        PeriodoAcademico invertido = new PeriodoAcademico("2026-1", Date.valueOf("2026-06-15"), Date.valueOf("2026-01-15"), "ACTIVO");
        assertFalse(Validaciones.esPeriodoValido(invertido));
        assertFalse(Validaciones.esPeriodoValido(
                new PeriodoAcademico("2026-1", Date.valueOf("2026-01-15"), Date.valueOf("2026-06-15"), "ABIERTO")));
    }

    @Test
    void cursos() {
        assertTrue(Validaciones.esCursoValido(new Curso("Cálculo", 3, 1, 1)));
        assertFalse(Validaciones.esCursoValido(new Curso("Cálculo", 0, 1, 1)));
        assertFalse(Validaciones.esCursoValido(new Curso("Cálculo", 11, 1, 1)));
        assertFalse(Validaciones.esCursoValido(new Curso("Cálculo", 3, 1, 0)));
    }

    @Test
    void secciones() {
        assertTrue(Validaciones.esSeccionValida(new Seccion(1, 1, 1, "A-101", 30)));
        assertFalse(Validaciones.esSeccionValida(new Seccion(1, 1, 1, "A-101", 0)));
        assertFalse(Validaciones.esSeccionValida(new Seccion(1, 1, 0, "A".repeat(51), 30)));
    }

    /** El curso, el periodo y el estado los deriva la sección: el formulario solo trae estudiante y sección. */
    @Test
    void matriculas() {
        assertTrue(Validaciones.esMatriculaValida(matricula(1, 2, null)));
        assertTrue(Validaciones.esMatriculaValida(matricula(1, 2, 3.5)));
        assertFalse(Validaciones.esMatriculaValida(matricula(1, 2, 5.1)));
        assertFalse(Validaciones.esMatriculaValida(matricula(0, 2, null)));
        assertFalse(Validaciones.esMatriculaValida(matricula(1, 0, null)));
        assertFalse(Validaciones.esMatriculaValida(matricula(1, 2, -0.1)));
    }

    @Test
    void asistencias() {
        assertTrue(Validaciones.esAsistenciaValida(new Asistencia(1, 1, HOY, "ASISTIO")));
        assertTrue(Validaciones.esAsistenciaValida(new Asistencia(1, 1, HOY, "falto")));
        assertFalse(Validaciones.esAsistenciaValida(new Asistencia(1, 1, HOY, "VAGANDO")));
        assertFalse(Validaciones.esAsistenciaValida(new Asistencia(1, 1, null, "ASISTIO")));
        assertFalse(Validaciones.esAsistenciaValida(
                new Asistencia(1, 1, Date.valueOf(LocalDate.now().plusDays(1)), "ASISTIO")));
    }

    @Test
    void calificaciones() {
        assertTrue(Validaciones.esCalificacionValida(new Calificacion(1, 1, 3.0, null, null)));
        assertFalse(Validaciones.esCalificacionValida(new Calificacion(1, 1, null, null, null)));
        assertFalse(Validaciones.esCalificacionValida(new Calificacion(1, 1, -0.1, 4.0, 4.0)));
        assertFalse(Validaciones.esCalificacionValida(new Calificacion(1, 0, 4.0, 4.0, 4.0)));
    }

    @Test
    void cuentasEnCreacion() {
        assertTrue(Validaciones.esCuentaValida(cuenta("ana", "ClaveLocal123", Sesion.ADMINISTRATIVO), true));
        assertFalse(Validaciones.esCuentaValida(cuenta("ana", "corta123", Sesion.ADMINISTRATIVO), true));
        assertFalse(Validaciones.esCuentaValida(cuenta("a b", "ClaveLocal123", Sesion.ADMINISTRATIVO), true));
        assertFalse(Validaciones.esCuentaValida(cuenta("ana", "ClaveLocal123", "DIRECTIVO"), true));
    }

    @Test
    void laFichaVinculadaDependeDelRol() {
        Cuenta estudiante = cuenta("ana", "ClaveLocal123", Sesion.ESTUDIANTE);
        assertFalse(Validaciones.esCuentaValida(estudiante, true));
        estudiante.setIdEstudiante(4);
        assertTrue(Validaciones.esCuentaValida(estudiante, true));

        Cuenta profesor = cuenta("juan", "ClaveLocal123", Sesion.PROFESOR);
        assertFalse(Validaciones.esCuentaValida(profesor, true));
        profesor.setIdProfesor(2);
        assertTrue(Validaciones.esCuentaValida(profesor, true));
    }

    @Test
    void edicionDeCuentaPuedeOmitirLaClave() {
        Cuenta cuenta = cuenta("ana", null, Sesion.ADMINISTRATIVO);
        assertTrue(Validaciones.esCuentaValida(cuenta, false));
        assertFalse(Validaciones.esCuentaValida(cuenta, true));
    }

    private static Matricula matricula(int estudiante, int seccion, Double nota) {
        Matricula m = new Matricula();
        m.setIdEstudiante(estudiante);
        m.setIdSeccion(seccion);
        m.setNota(nota);
        return m;
    }

    private static Cuenta cuenta(String usuario, String clave, String rol) {
        Cuenta c = new Cuenta();
        c.setUsername(usuario);
        c.setPassword(clave);
        c.setRol(rol);
        return c;
    }
}
