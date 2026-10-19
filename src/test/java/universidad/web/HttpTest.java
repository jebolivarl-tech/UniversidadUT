package universidad.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import java.sql.Date;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class HttpTest {

    private static Context conQueryParam(String nombre, String valor) {
        Context ctx = mock(Context.class);
        when(ctx.queryParam(nombre)).thenReturn(valor);
        return ctx;
    }

    @Test
    void enteroQueryOpcional() {
        assertNull(Http.enteroQuery(conQueryParam("curso", null), "curso"));
        assertNull(Http.enteroQuery(conQueryParam("curso", "  "), "curso"));
        assertEquals(7, Http.enteroQuery(conQueryParam("curso", "7"), "curso"));
    }

    @Test
    void enteroQueryRechazaValoresInvalidos() {
        ApiException texto = assertThrows(ApiException.class,
                () -> Http.enteroQuery(conQueryParam("curso", "abc"), "curso"));
        assertEquals(400, texto.getStatus());
        assertThrows(ApiException.class, () -> Http.enteroQuery(conQueryParam("curso", "0"), "curso"));
    }

    @Test
    void fechaQueryEnFormatoIso() {
        assertNull(Http.fechaQuery(conQueryParam("fecha", ""), "fecha"));
        assertEquals(Date.valueOf("2026-10-16"), Http.fechaQuery(conQueryParam("fecha", "2026-10-16"), "fecha"));
        ApiException invalida = assertThrows(ApiException.class,
                () -> Http.fechaQuery(conQueryParam("fecha", "16/10/2026"), "fecha"));
        assertEquals(400, invalida.getStatus());
    }

    @Test
    void distingueDuplicadoDeClaveForanea() {
        assertTrue(Http.esDuplicado(new SQLException("", "23000", 1062)));
        assertFalse(Http.esReferenciaInexistente(new SQLException("", "23000", 1062)));

        assertTrue(Http.esReferenciaInexistente(new SQLException("", "23000", 1452)));
        assertFalse(Http.esDuplicado(new SQLException("", "23000", 1452)));
    }
}
