package universidad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;

/** Las cuentas de asistencia y de notas, sin base de datos de por medio. */
class ReglasAcademicasTest {

    @Test
    void soloLaFaltaNoJustificadaDescuenta() {
        assertTrue(ReglasAcademicas.esFaltaNoJustificada("FALTO"));
        assertTrue(ReglasAcademicas.esFaltaNoJustificada(" falto "));
        assertFalse(ReglasAcademicas.esFaltaNoJustificada(Asistencia.JUSTIFICADO));
        assertFalse(ReglasAcademicas.esFaltaNoJustificada(Asistencia.TARDANZA));
        assertFalse(ReglasAcademicas.esFaltaNoJustificada(null));
    }

    @Test
    void sinControlesNoHayNadaPerdido() {
        assertEquals(1.0, ReglasAcademicas.porcentajeDeAsistencia(List.of()));
        assertFalse(ReglasAcademicas.pierdePorAsistencia(List.of()));
    }

    /** Con menos de cuatro controles la asistencia no puede sentenciar a nadie. */
    @Test
    void losPrimerosTresControlesNoPierdenElCurso() {
        assertFalse(ReglasAcademicas.pierdePorAsistencia(controles("FALTO", "FALTO", "FALTO")));
        assertTrue(ReglasAcademicas.pierdePorAsistencia(controles("FALTO", "FALTO", "FALTO", "FALTO")));
    }

    @Test
    void elUmbralEsElSetentaYCincoPorCiento() {
        // 4 de 5: 80 % de asistencia, por encima del mínimo.
        assertFalse(ReglasAcademicas.pierdePorAsistencia(controles(
                "ASISTIO", "ASISTIO", "ASISTIO", "ASISTIO", "FALTO")));
        // 3 de 5: 60 %, por debajo.
        assertTrue(ReglasAcademicas.pierdePorAsistencia(controles(
                "ASISTIO", "ASISTIO", "ASISTIO", "FALTO", "FALTO")));
        assertEquals(0.6, ReglasAcademicas.porcentajeDeAsistencia(controles(
                "ASISTIO", "ASISTIO", "ASISTIO", "FALTO", "FALTO")));
    }

    @Test
    void laTardanzaYElJustificadoCuentanComoAsistencia() {
        assertEquals(1.0, ReglasAcademicas.porcentajeDeAsistencia(controles("TARDANZA", "JUSTIFICADO")));
        assertFalse(ReglasAcademicas.pierdePorAsistencia(controles("TARDANZA", "JUSTIFICADO", "ASISTIO", "ASISTIO")));
    }

    @Test
    void lasNotasSePublicanConDosDecimales() {
        assertEquals(3.8, ReglasAcademicas.redondearNota(3.799999));
        assertEquals(3.05, ReglasAcademicas.redondearNota(Calificacion.promedio(3.5, 4.0, 2.0)));
    }

    @Test
    void elTopeDeCreditosYElUmbralSonLosQueDiceLaDocumentacion() {
        assertEquals(20, ReglasAcademicas.CREDITOS_MAXIMOS_POR_PERIODO);
        assertEquals(0.75, ReglasAcademicas.PORCENTAJE_MINIMO_ASISTENCIA);
        assertEquals(4, ReglasAcademicas.CONTROLES_MINIMOS_PARA_PERDER_POR_ASISTENCIA);
    }

    private static List<Asistencia> controles(String... estados) {
        Date hoy = Date.valueOf(LocalDate.now());
        List<Asistencia> lista = new ArrayList<>();
        for (int i = 0; i < estados.length; i++) {
            Asistencia a = new Asistencia(1, 1, hoy, estados[i]);
            a.setIdAsistencia(i + 1);
            lista.add(a);
        }
        return lista;
    }
}
