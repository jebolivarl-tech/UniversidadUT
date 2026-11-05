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

/**
 * La forma de los datos de cada módulo que la API REST revisa antes de llamar al DAO — la clase
 * que escribe en la base de datos por SQL. Las pruebas van rompiendo una parte a la vez, con
 * JUnit y sin tocar la base.
 */
class ModulosValidacionesTest {

    private static final Date HOY = Date.valueOf(LocalDate.now());

    @Test
    void facultaes() {
        // el nombre es obligatorio y no puede pasar de 100 caracteres
        assertTrue(Validaciones.esFacultadValida(new Facultad("Ingeniería", null)));
        assertFalse(Validaciones.esFacultadValida(new Facultad(" ", "x")));
        assertFalse(Validaciones.esFacultadValida(new Facultad("n".repeat(101), null)));
    }

    @Test
    void profesores() {
        // hace falta una facultad real (id mayor que 0) y un correo con pinta de correo
        assertTrue(Validaciones.esProfesorValido(new Profesor("Ana", "Ruiz", "ana@ut.edu.co", 1)));
        assertFalse(Validaciones.esProfesorValido(new Profesor("Ana", "Ruiz", "ana@ut.edu.co", 0)));
        assertFalse(Validaciones.esProfesorValido(new Profesor("Ana", "Ruiz", "correo-malo", 1)));
    }

    @Test
    void periodos() {
        // el cierre no puede caer antes de la apertura y el estado tiene que ser uno de los que usa la app
        assertTrue(Validaciones.esPeriodoValido(
                new PeriodoAcademico("2026-1", Date.valueOf("2026-01-15"), Date.valueOf("2026-06-15"), "ACTIVO")));
        PeriodoAcademico invertido = new PeriodoAcademico("2026-1", Date.valueOf("2026-06-15"), Date.valueOf("2026-01-15"), "ACTIVO");
        assertFalse(Validaciones.esPeriodoValido(invertido));
        assertFalse(Validaciones.esPeriodoValido(
                new PeriodoAcademico("2026-1", Date.valueOf("2026-01-15"), Date.valueOf("2026-06-15"), "ABIERTO")));
    }

    @Test
    void cursos() {
        // créditos entre 1 y 10 y un profesor asignado de verdad
        assertTrue(Validaciones.esCursoValido(new Curso("Cálculo", 3, 1, 1)));
        assertFalse(Validaciones.esCursoValido(new Curso("Cálculo", 0, 1, 1)));
        assertFalse(Validaciones.esCursoValido(new Curso("Cálculo", 11, 1, 1)));
        assertFalse(Validaciones.esCursoValido(new Curso("Cálculo", 3, 1, 0)));
    }

    @Test
    void secciones() {
        // cupo mayor que 0 y aula corta; los ids de curso, profesor y periodo deben existir
        assertTrue(Validaciones.esSeccionValida(new Seccion(1, 1, 1, "A-101", 30)));
        assertFalse(Validaciones.esSeccionValida(new Seccion(1, 1, 1, "A-101", 0)));
        assertFalse(Validaciones.esSeccionValida(new Seccion(1, 1, 0, "A".repeat(51), 30)));
    }

    /** El curso, el periodo y el estado los deriva la sección: el formulario solo trae estudiante y sección, y la nota, si viene, va de 0 a 5. */
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
        // el estado puede llegar en minúsculas, pero no uno inventado; la fecha debe existir y no ser futura
        assertTrue(Validaciones.esAsistenciaValida(new Asistencia(1, 1, HOY, "ASISTIO")));
        assertTrue(Validaciones.esAsistenciaValida(new Asistencia(1, 1, HOY, "falto")));
        assertFalse(Validaciones.esAsistenciaValida(new Asistencia(1, 1, HOY, "VAGANDO")));
        assertFalse(Validaciones.esAsistenciaValida(new Asistencia(1, 1, null, "ASISTIO")));
        assertFalse(Validaciones.esAsistenciaValida(
                new Asistencia(1, 1, Date.valueOf(LocalDate.now().plusDays(1)), "ASISTIO")));
    }

    @Test
    void calificaciones() {
        // hace falta al menos la primera nota y todas las que vengan deben ir de 0 a 5
        assertTrue(Validaciones.esCalificacionValida(new Calificacion(1, 1, 3.0, null, null)));
        assertFalse(Validaciones.esCalificacionValida(new Calificacion(1, 1, null, null, null)));
        assertFalse(Validaciones.esCalificacionValida(new Calificacion(1, 1, -0.1, 4.0, 4.0)));
        assertFalse(Validaciones.esCalificacionValida(new Calificacion(1, 0, 4.0, 4.0, 4.0)));
    }

    @Test
    void cuentasEnCreacion() {
        // al crear la cuenta se exige clave suficientemente larga, usuario sin espacios y un rol conocido
        assertTrue(Validaciones.esCuentaValida(cuenta("ana", "ClaveLocal123", Sesion.ADMINISTRATIVO), true));
        assertFalse(Validaciones.esCuentaValida(cuenta("ana", "corta123", Sesion.ADMINISTRATIVO), true));
        assertFalse(Validaciones.esCuentaValida(cuenta("a b", "ClaveLocal123", Sesion.ADMINISTRATIVO), true));
        assertFalse(Validaciones.esCuentaValida(cuenta("ana", "ClaveLocal123", "DIRECTIVO"), true));
    }

    @Test
    void laFichaVinculadaDependeDelRol() {
        // los roles ESTUDIANTE y PROFESOR solo valen si la cuenta ya trae unida su ficha correspondiente
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
        // al editar se permite dejar la clave vacía para no cambiarla; al crear la cuenta no
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
