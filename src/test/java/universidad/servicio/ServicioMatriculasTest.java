package universidad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import universidad.modelo.Calificacion;
import universidad.modelo.Matricula;
import universidad.modelo.PeriodoAcademico;

/**
 * Reglas de inscripción: la sección manda, el periodo tiene que estar abierto, la carrera tiene que
 * coincidir, el cupo se respeta y los créditos tienen techo.
 */
class ServicioMatriculasTest {

    private final Escenario escenario = new Escenario();

    @Test
    void elCursoYelPeriodoSalenDeLaSeccion() {
        Matricula nueva = escenario.inscripcion(null);
        escenario.matriculasServicio.preparar(nueva);

        assertEquals(Escenario.CURSO, nueva.getIdCurso());
        assertEquals(Escenario.PERIODO_NOMBRE, nueva.getPeriodo());
        assertEquals(Matricula.EN_CURSO, nueva.getEstado());
    }

    @Test
    void unPeriodoCerradoNoAdmiteInscripciones() {
        escenario.periodoEn(PeriodoAcademico.CERRADO);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(escenario.inscripcion(null)));
        assertEquals(409, ex.getHttpStatus());
        assertEquals("El periodo 2026-2 está CERRADO: no se admiten matrículas ni cambios de sección.",
                ex.getMessage());
    }

    @Test
    void unCursoDeOtraCarreraNoSePuedeMatricular() {
        escenario.cursoDeOtraCarrera();

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(escenario.inscripcion(null)));
        assertEquals("El curso «Cálculo» no pertenece a la carrera del estudiante: no puede matricularlo.",
                ex.getMessage());
    }

    @Test
    void laSeccionLlenaRechazaLaInscripcion() {
        escenario.seccionCon("B-101", 30);
        escenario.ocupacionDeSeccion(30);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(escenario.inscripcion(null)));
        assertEquals("La sección Cálculo · B-101 · 2026-2 (0/30) está llena (30/30 cupos).", ex.getMessage());
    }

    @Test
    void elMismoEstudianteNoSeMatriculaDosVecesEnLaSeccion() {
        escenario.estaMatriculado();

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(escenario.inscripcion(null)));
        assertEquals("El estudiante ya está matriculado en esa sección.", ex.getMessage());
    }

    @Test
    void elMismoCursoNoSeCursaDosVecesEnElPeriodoAunqueCambieDeGrupo() {
        escenario.yaInscritoEnOtroGrupoDelCurso();

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(escenario.inscripcion(null)));
        assertEquals(409, ex.getHttpStatus());
        assertEquals("El estudiante ya cursa «Cálculo» en 2026-2: elija un curso distinto o cambie de periodo.",
                ex.getMessage());
    }

    @Test
    void elTopeDeCreditosCuentaLosYaInscritos() {
        escenario.yaInscritoEnCursosDeCreditos(12, 6);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(escenario.inscripcion(null)));
        assertEquals("El estudiante llegaría a 21 créditos en 2026-2 y el máximo por periodo es de 20.",
                ex.getMessage());
    }

    @Test
    void cambiarDeSeccionCuentaComoInscripcionNueva() {
        escenario.periodoEn(PeriodoAcademico.CERRADO);
        escenario.matriculaEnOtraSeccion(100, 9);

        Matricula cambio = escenario.inscripcion(null);
        cambio.setIdMatricula(100);
        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(cambio));
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void enElMismoGrupoSePuedeActualizarLaNotaConElPeriodoCerrado() {
        escenario.periodoEn(PeriodoAcademico.CERRADO);
        Matricula guardada = escenario.matriculaGuardada(100, 4.5);

        escenario.matriculasServicio.preparar(guardada);

        assertEquals(Matricula.APROBADO, guardada.getEstado());
    }

    @Test
    void laFaltaDeAsistenciaMandaSobreLaNota() {
        escenario.controlesDeAsistencia("FALTO", "FALTO", "FALTO", "ASISTIO");
        Matricula guardada = escenario.matriculaGuardada(100, 5.0);

        escenario.matriculasServicio.preparar(guardada);

        assertEquals(Matricula.PERDIDO_POR_FALTA_DE_ASISTENCIA, guardada.getEstado());
    }

    @Test
    void losCuposDisponiblesSonLosQueFaltan() {
        escenario.seccionCon("B-101", 30);
        escenario.ocupacionDeSeccion(12);

        assertEquals(18, escenario.matriculasServicio.cuposDisponibles(Escenario.SECCION));
    }

    @Test
    void laDefinitivaCompletaSeCopiaSobreLaMatricula() throws SQLException {
        escenario.estaMatriculado();
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 3.5, 4.0, 3.8);

        Matricula actualizada = escenario.matriculasServicio.sincronizarConCalificacion(notas);

        assertEquals(3.77, actualizada.getNota());
        assertEquals(Matricula.APROBADO, actualizada.getEstado());
        verify(escenario.matriculas).actualizar(argThat(m -> m.getNota() == 3.77));
    }

    @Test
    void sinLasTresNotasLaMatriculaNoSeToca() {
        escenario.estaMatriculado();
        Calificacion parcial = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 3.5, null, null);

        assertNull(escenario.matriculasServicio.sincronizarConCalificacion(parcial));
    }

    @Test
    void sinMatriculaNoHayNadaQueSincronizar() {
        escenario.noEstaMatriculado();
        Calificacion notas = new Calificacion(Escenario.ESTUDIANTE, Escenario.SECCION, 3.5, 4.0, 3.8);

        assertNull(escenario.matriculasServicio.sincronizarConCalificacion(notas));
    }
}
