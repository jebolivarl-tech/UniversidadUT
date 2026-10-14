package universidad.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void bloqueaTrasAlcanzarElMaximoYSeLiberaAlPasarLaVentana() {
        AtomicLong reloj = new AtomicLong(0);
        LoginRateLimiter limiter = new LoginRateLimiter(3, 60_000, reloj::get);

        for (int i = 0; i < 2; i++) limiter.registrarFallo("a");
        assertEquals(0, limiter.segundosRestantes("a"));

        limiter.registrarFallo("a");
        assertTrue(limiter.segundosRestantes("a") > 0);
        assertEquals(0, limiter.segundosRestantes("otra-clave"));

        reloj.set(60_001);
        assertEquals(0, limiter.segundosRestantes("a"));
    }

    @Test
    void reiniciarLimpiaLosFallos() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 60_000);
        limiter.registrarFallo("a");
        assertTrue(limiter.segundosRestantes("a") > 0);
        limiter.reiniciar("a");
        assertEquals(0, limiter.segundosRestantes("a"));
    }
}
