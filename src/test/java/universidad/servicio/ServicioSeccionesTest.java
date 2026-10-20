package universidad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import universidad.modelo.Matricula;
import universidad.modelo.Seccion;

/**
 * Reglas de apertura de grupos: el aula no se repite, el cupo no se falsea y un grupo con
 * matriculados no se borra — igual que el curso que los agrupa.
 */
class ServicioSeccionesTest {

    private final Escenario escenario = new Escenario();

    @Test
    void unGrupoNuevoDelCursoYPeriodoCorrectosSeAbre() {
        Seccion nueva = escenario.seccion(0, "A-202", 25);

        escenario.seccionesServicio.preparar(nueva);

        assertEquals("A-202", nueva.getAula());
    }

    @Test
    void laMismaAulaNoSeRepiteEnElCursoYElPeriodo() {
        escenario.otraSeccionEnElPeriodo(9, "A-202");
        Seccion nueva = escenario.seccion(0, "a-202", 25);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.seccionesServicio.preparar(nueva));
        assertEquals(409, ex.getHttpStatus());
        assertEquals("Ya existe una sección del mismo curso con el aula a-202 en este periodo.", ex.getMessage());
    }

    @Test
    void elCupoNoPuedeBajarDeLosQueYaMatricularon() {
        Seccion existente = escenario.seccion(Escenario.SECCION, "B-101", 20);
        escenario.ocupacionDeSeccion(25);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.seccionesServicio.preparar(existente));
        assertEquals("La sección ya tiene 25 matriculado(s): el cupo máximo no puede bajar de esa cifra.",
                ex.getMessage());
    }

    @Test
    void unGrupoConMatriculadosNoSeElimina() {
        escenario.ocupacionDeSeccion(4);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.seccionesServicio.validarEliminacion(Escenario.SECCION));
        assertEquals("La sección Cálculo · B-101 · 2026-2 (0/30) tiene 4 estudiante(s) matriculado(s): "
                + "elimine primero las matrículas.", ex.getMessage());
    }

    @Test
    void unGrupoVacioSePuedeEliminar() {
        escenario.ocupacionDeSeccion(0);

        escenario.seccionesServicio.validarEliminacion(Escenario.SECCION);
    }

    @Test
    void unCursoConMatriculasNoSeElimina() {
        escenario.matriculasEnCurso(2);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.seccionesServicio.validarEliminacionDeCurso(Escenario.CURSO));
        assertEquals(409, ex.getHttpStatus());
        assertEquals("El curso «Cálculo» tiene 2 matrícula(s) en sus grupos: elimine primero las matrículas.",
                ex.getMessage());
    }

    @Test
    void unCursoSinMatriculasSePuedeEliminar() {
        escenario.matriculasEnCurso(0);

        escenario.seccionesServicio.validarEliminacionDeCurso(Escenario.CURSO);
    }

    @Test
    void lasSeccionesInscribiblesSonLasDelCatalogoDelEstudiante() {
        when(escenario.secciones.listarInscribiblesPara(Escenario.ESTUDIANTE))
                .thenReturn(List.of(escenario.seccion(9, "A-202", 25)));

        List<Seccion> inscribibles = escenario.seccionesServicio.inscribiblesPara(Escenario.ESTUDIANTE);

        assertEquals(1, inscribibles.size());
    }

    @Test
    void elTopeDeCreditosTambienFiltraLosGruposQueSeOfrecen() {
        escenario.inscritasEnElPeriodo(16);                                  // 16 créditos ya cursados
        when(escenario.cursos.leerPorId(70)).thenReturn(escenario.curso(70, "Taller", 5, Escenario.CARRERA));
        Seccion cabe = escenario.seccion(9, "A-202", 25);                    // 3 créditos: va a 19
        Seccion noCabe = escenario.seccion(10, "A-203", 25);                 // 5 créditos: pasaría de 20
        noCabe.setIdCurso(70);
        when(escenario.secciones.listarInscribiblesPara(Escenario.ESTUDIANTE)).thenReturn(List.of(cabe, noCabe));

        List<Seccion> inscribibles = escenario.seccionesServicio.inscribiblesPara(Escenario.ESTUDIANTE);

        assertEquals(List.of(9), inscribibles.stream().map(Seccion::getIdSeccion).toList());
    }

    @Test
    void unEstudianteQueNoExisteNoPuedeConsultarSuCatalogo() {
        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.seccionesServicio.inscribiblesPara(999));
        assertEquals(400, ex.getHttpStatus());
        assertEquals("No se encontró el estudiante #999.", ex.getMessage());
    }

    @Test
    void unaSeccionQueNoExisteSeReportaComoReferenciaRota() {
        Matricula m = new Matricula();
        m.setIdEstudiante(Escenario.ESTUDIANTE);
        m.setIdSeccion(777);

        ReglaVioladaException ex = assertThrows(ReglaVioladaException.class,
                () -> escenario.matriculasServicio.preparar(m));
        assertEquals("No se encontró la sección #777.", ex.getMessage());
    }
}
