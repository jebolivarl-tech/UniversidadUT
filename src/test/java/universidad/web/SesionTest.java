package universidad.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

/**
 * La sesión es lo que el servidor recuerda de un usuario después del login: ahí guarda si entró
 * y con qué rol. Sesion consulta eso antes de dejar pasar cada petición a la API REST y responde
 * 401 (nadie ha hecho login) o 403 (el rol no está en la lista permitida), códigos de HTTP.
 * Mockito arma imitaciones del Context de Javalin, así que no hace falta levantar el servidor.
 */
class SesionTest {

    private static Context contexto(Boolean autenticado, String rol) {
        // petición fingida cuya sesión tiene el valor de login y el rol que la prueba diga
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
        // con el rol justo en la lista autorizada el servicio deja seguir, sin excepciones
        assertDoesNotThrow(() -> Sesion.requerirRol(contexto(true, Sesion.ADMINISTRATIVO), Sesion.ADMINISTRATIVO));
        assertDoesNotThrow(() -> Sesion.requerirRol(contexto(true, Sesion.PROFESOR), Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
    }

    @Test
    void soloTrueExactoCuentaComoAutenticado() {
        // solo vale el "sí" que el login dejó en la sesión; un valor viejo o ausente no autentica a nadie
        assertEquals(true, Sesion.estaAutenticado(contexto(true, null)));
        assertEquals(false, Sesion.estaAutenticado(contexto(false, null)));
        assertEquals(false, Sesion.estaAutenticado(contexto(null, null)));
    }
}
