package universidad.web;

import java.time.LocalDate;
import universidad.modelo.Carrera;
import universidad.modelo.Estudiante;

/** Reglas de validación de entrada (alineadas con los tamaños de columna de la base de datos). */
public final class Validaciones {

    private static final String PATRON_EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

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

    private static boolean textoValido(String valor, int maximo) {
        return valor != null && !valor.isBlank() && valor.length() <= maximo;
    }
}
