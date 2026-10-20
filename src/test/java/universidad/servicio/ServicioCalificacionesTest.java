package universidad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import universidad.modelo.Asistencia;
import universidad.modelo.Calificacion;
import universidad.modelo.Matricula;

/** Evaluación continua: el promedio y el estado los fija el servidor, nunca quien escribe la nota. */
class ServicioCalificacionesTest {

    private final Escenario escenario = new Escenario();

    @Test
    void elPromedioSaleDeLosTresMomentos() {
        escenario.estaMatriculado();
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 3.5, 4.0, 3.8);

        escenario.calificacionesServicio.preparar(notas);

        assertEquals(3.77, notas.getPromedioFinal());
        assertEquals(Calificacion.APROBADO, notas.getEstado());
    }

    @Test
    void conUnaNotaFaltanteElCursoSigueEnCurso() {
        escenario.estaMatriculado();
        Calificacion parcial = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 3.5, 4.0, null);

        escenario.calificacionesServicio.preparar(parcial);

        assertEquals(Calificacion.EN_CURSO, parcial.getEstado());
    }

    @Test
    void unaDefinitivaBajaReprueba() {
        escenario.estaMatriculado();
        Calificacion baja = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 2.0, 2.0, 3.0);

        escenario.calificacionesServicio.preparar(baja);

        assertEquals(2.4, baja.getPromedioFinal());
        assertEquals(Calificacion.REPROBADO, baja.getEstado());
    }

    @Test
    void laNotaAltaNoSalvaLaAsistencia() {
        escenario.estaMatriculado();
        escenario.controlesDeAsistencia("FALTO", "FALTO", "FALTO", "FALTO");
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 5.0, 5.0, 5.0);

        escenario.calificacionesServicio.preparar(notas);

        assertEquals(Calificacion.PERDIDO_POR_FALTA_DE_ASISTENCIA, notas.getEstado());
    }

    @Test
    void noSeCalificaAQuienNoEstaMatriculado() {
        escenario.noEstaMatriculado();
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 4.0, 4.0, 4.0);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.calificacionesServicio.preparar(notas));
        assertEquals("María Rojas no está matriculado en la sección Cálculo · B-101 · 2026-2 (0/30): "
                + "regístrelo antes de cargarle notas.", ex.getMessage());
    }

    @Test
    void guardarLaDefinitivaCierraElCirculoConLaMatricula() throws SQLException {
        Matricula matricula = escenario.matriculaGuardada(100, null);
        when(escenario.matriculas.leerPorEstudianteYSeccion(Escenario.ESTUDIANTE, Escenario.SECCION))
                .thenReturn(matricula);
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 3.5, 4.0, 3.8);

        escenario.calificacionesServicio.preparar(notas);
        escenario.calificacionesServicio.propagarNotaDefinitiva(notas);

        assertEquals(3.77, matricula.getNota());
        assertEquals(Matricula.APROBADO, matricula.getEstado());
        verify(escenario.matriculas).actualizar(matricula);
    }

    @Test
    void elEstadoDerivadoNoDejaQueElClienteLoImponga() {
        escenario.estaMatriculado();
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 5.0, 5.0, 5.0);
        notas.setEstado(Asistencia.JUSTIFICADO);

        escenario.calificacionesServicio.preparar(notas);

        assertEquals(Calificacion.APROBADO, notas.getEstado());
    }
}
