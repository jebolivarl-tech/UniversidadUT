package universidad.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

class SesionTest {

    private static Context contexto(Boolean autenticado, String rol) {
        Context ctx = mock(Context.class);
        when(ctx.sessionAttribute("authenticated")).thenReturn(autenticado);
        when(ctx.sessionAttribute("rol")).thenReturn(rol);
        return ctx;
    }

    @Test
    void sinSesionDevuelve401() {
        ApiException ex = assertThrows(ApiException.class,
                () -> Sesion.requerirRol(contexto(null, null), Sesion.ADMINISTRATIVO));
        assertEquals(401, ex.getStatus());
    }

    @Test
    void rolNoPermitidoDevuelve403() {
        ApiException ex = assertThrows(ApiException.class,
                () -> Sesion.requerirRol(contexto(true, Sesion.ESTUDIANTE), Sesion.ADMINISTRATIVO));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void rolPermitidoPasa() {
        assertDoesNotThrow(() -> Sesion.requerirRol(contexto(true, Sesion.ADMINISTRATIVO), Sesion.ADMINISTRATIVO));
        assertDoesNotThrow(() -> Sesion.requerirRol(contexto(true, Sesion.PROFESOR), Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
    }

    @Test
    void soloTrueExactoCuentaComoAutenticado() {
        assertEquals(true, Sesion.estaAutenticado(contexto(true, null)));
        assertEquals(false, Sesion.estaAutenticado(contexto(false, null)));
        assertEquals(false, Sesion.estaAutenticado(contexto(null, null)));
    }
}
