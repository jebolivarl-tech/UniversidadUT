package universidad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Date;
import org.junit.jupiter.api.Test;
import universidad.modelo.Asistencia;
import universidad.modelo.Matricula;

/** Pase de lista: solo a matriculados, solo en fechas del periodo y un solo registro por día. */
class ServicioAsistenciasTest {

    private final Escenario escenario = new Escenario();

    @Test
    void elEstadoSeGuardaEnMayusculas() {
        escenario.estaMatriculado();
        Asistencia control = new Asistencia(Escenario.ESTUDIANTE, Escenario.SECCION, Escenario.HOY, "falto");

        escenario.asistenciasServicio.preparar(control);

        assertEquals(Asistencia.FALTO, control.getEstado());
    }

    @Test
    void unNoMatriculadoNoPuedeAparecerEnLaLista() {
        escenario.noEstaMatriculado();
        Asistencia control = new Asistencia(Escenario.ESTUDIANTE, Escenario.SECCION, Escenario.HOY, "ASISTIO");

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.asistenciasServicio.preparar(control));
        assertEquals(409, ex.getHttpStatus());
        assertEquals("María Rojas no está matriculado en la sección Cálculo · B-101 · 2026-2 (0/30) "
                + "y por eso no se le puede pasar lista.", ex.getMessage());
    }

    @Test
    void laFechaTieneQueCaerDentroDelPeriodo() {
        escenario.estaMatriculado();
        Asistencia control = new Asistencia(Escenario.ESTUDIANTE, Escenario.SECCION,
                Date.valueOf("2026-05-01"), "ASISTIO");

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.asistenciasServicio.preparar(control));
        assertEquals("La fecha 2026-05-01 está fuera del periodo 2026-2 (2026-07-15 a 2026-12-15).",
                ex.getMessage());
    }

    @Test
    void unEstudianteSoloTieneUnControlPorDia() {
        escenario.estaMatriculado();
        escenario.controlesDeAsistencia("ASISTIO");
        Asistencia repetido = new Asistencia(Escenario.ESTUDIANTE, Escenario.SECCION, Escenario.HOY, "FALTO");

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.asistenciasServicio.preparar(repetido));
        assertEquals("Ese estudiante ya tiene un registro de asistencia el " + Escenario.HOY
                + " en esta sección.", ex.getMessage());
    }

    @Test
    void corregirElControlDelDiaNoSeChoqueConsigoMismo() {
        escenario.estaMatriculado();
        escenario.controlesDeAsistencia("ASISTIO");
        Asistencia corregido = new Asistencia(Escenario.ESTUDIANTE, Escenario.SECCION, Escenario.HOY, "FALTO");
        corregido.setIdAsistencia(1);

        escenario.asistenciasServicio.preparar(corregido);

        assertEquals(Asistencia.FALTO, corregido.getEstado());
    }

    @Test
    void losTresPrimerosControlesNoSentencianAlEstudiante() {
        escenario.estaMatriculado();
        escenario.controlesDeAsistencia("FALTO", "FALTO", "FALTO");
        Matricula matricula = escenario.matriculaGuardada(100, 4.0);

        escenario.matriculasServicio.preparar(matricula);

        assertEquals(Matricula.APROBADO, matricula.getEstado());
    }
}
