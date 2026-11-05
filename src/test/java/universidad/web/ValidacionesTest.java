package universidad.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import universidad.modelo.Carrera;
import universidad.modelo.Estudiante;

/**
 * La forma básica del estudiante y de la carrera antes de que el DAO — la clase que escribe en la
 * base de datos por SQL — intente guardarlos: nombre con medida, correo con pinta, fecha de
 * nacimiento que no caiga en el futuro y carrera real. JUnit ejecuta estas comprobaciones sin
 * base de datos de por medio.
 */
class ValidacionesTest {

    private static Estudiante valido() {
        return new Estudiante("Ana", "Gómez", "ana@mail.com", Date.valueOf("2003-11-22"), 1);
    }

    @Test
    void aceptaEstudianteValido() {
        assertTrue(Validaciones.esEstudianteValido(valido()));
    }

    @Test
    void rechazaEstudiantesInvalidos() {
        // cada línea rompe una sola cosa de un estudiante por lo demás correcto
        assertFalse(Validaciones.esEstudianteValido(null));

        Estudiante e = valido(); e.setNombre(" ");
        assertFalse(Validaciones.esEstudianteValido(e));

        e = valido(); e.setNombre("x".repeat(61));
        assertFalse(Validaciones.esEstudianteValido(e));

        e = valido(); e.setEmail("no-es-email");
        assertFalse(Validaciones.esEstudianteValido(e));

        e = valido(); e.setFechaNacimiento(null);
        assertFalse(Validaciones.esEstudianteValido(e));

        e = valido(); e.setFechaNacimiento(Date.valueOf(LocalDate.now().plusDays(1)));
        assertFalse(Validaciones.esEstudianteValido(e));

        e = valido(); e.setIdCarrera(0);
        assertFalse(Validaciones.esEstudianteValido(e));
    }

    @Test
    void validaCarreras() {
        // una carrera necesita nombre (hasta 100 caracteres) y una facultad real por detrás
        assertTrue(Validaciones.esCarreraValida(new Carrera("Ingeniería", 1)));
        assertFalse(Validaciones.esCarreraValida(null));
        assertFalse(Validaciones.esCarreraValida(new Carrera(null, 1)));
        assertFalse(Validaciones.esCarreraValida(new Carrera(" \t ", 1)));
        assertFalse(Validaciones.esCarreraValida(new Carrera("Ingeniería", 0)));
        assertFalse(Validaciones.esCarreraValida(new Carrera("x".repeat(101), 1)));
    }
}
