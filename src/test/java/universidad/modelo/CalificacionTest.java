package universidad.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CalificacionTest {

    @Test
    void promedioPonderadoTreintaTreintaCuarenta() {
        assertEquals(3.4, Calificacion.promedio(3.0, 3.0, 4.0), 0.0001);
        assertEquals(5.0, Calificacion.promedio(5.0, 5.0, 5.0), 0.0001);
    }

    @Test
    void sinExamenCuentaComoCeroHastaRegistrarlo() {
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
        assertEquals(Calificacion.APROBADO, Calificacion.estado(3.0, 3.0, 3.0));
        assertEquals(Calificacion.REPROBADO, Calificacion.estado(2.9, 3.0, 3.0));
    }
}
