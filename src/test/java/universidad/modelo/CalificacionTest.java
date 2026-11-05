package universidad.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Las cuentas de la nota final, hechas sobre números sueltos y sin base de datos de por medio:
 * los tres momentos pesan 30 %, 30 % y 40 %, y se aprueba con promedio de 3.0 o más.
 * JUnit es la librería de pruebas de Java que ejecuta estas comprobaciones.
 */
class CalificacionTest {

    @Test
    void promedioPonderadoTreintaTreintaCuarenta() {
        assertEquals(3.4, Calificacion.promedio(3.0, 3.0, 4.0), 0.0001);
        assertEquals(5.0, Calificacion.promedio(5.0, 5.0, 5.0), 0.0001);
    }

    @Test
    void sinExamenCuentaComoCeroHastaRegistrarlo() {
        // sin la nota del examen el promedio solo suma los dos parciales: 0.9 + 0.9 = 1.8
        assertEquals(1.8, Calificacion.promedio(3.0, 3.0, null), 0.0001);
    }

    @Test
    void estadoSoloSeDefineConLasTresNotas() {
        assertEquals(Calificacion.EN_CURSO, Calificacion.estado(3.0, 3.0, null));
        assertEquals(Calificacion.APROBADO, Calificacion.estado(3.0, 3.0, 3.0));
        assertEquals(Calificacion.REPROBADO, Calificacion.estado(2.0, 2.0, 2.0));
    }

    @Test
    void apruebaJustoEnElUmbral() {
        // el 2.9 da promedio 2.97: apenas por debajo del 3.0, y eso ya es reprueba
        assertEquals(Calificacion.APROBADO, Calificacion.estado(3.0, 3.0, 3.0));
        assertEquals(Calificacion.REPROBADO, Calificacion.estado(2.9, 3.0, 3.0));
    }
}
