package universidad.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * El candado del login: tras unos cuantos intentos fallidos se bloquea esa clave (usuario y equipo
 * que intenta entrar) durante una ventana de tiempo, para que nadie pruebe claves una por una
 * contra la API REST. La prueba le pasa al candado un reloj falso que ella misma adelanta, así no
 * tiene que esperar los 60 segundos reales. JUnit ejecuta estas comprobaciones.
 */
class LoginRateLimiterTest {

    @Test
    void bloqueaTrasAlcanzarElMaximoYSeLiberaAlPasarLaVentana() {
        AtomicLong reloj = new AtomicLong(0);
        LoginRateLimiter limiter = new LoginRateLimiter(3, 60_000, reloj::get);

        for (int i = 0; i < 2; i++) limiter.registrarFallo("a");
        assertEquals(0, limiter.segundosRestantes("a"));

        limiter.registrarFallo("a");
        // el tercer fallo llena el cupo de 3: desde ahí hay tiempo de espera pendiente
        assertTrue(limiter.segundosRestantes("a") > 0);
        // el bloqueo de "a" no castiga a las demás claves
        assertEquals(0, limiter.segundosRestantes("otra-clave"));

        reloj.set(60_001);
        assertEquals(0, limiter.segundosRestantes("a"));
    }

    @Test
    void reiniciarLimpiaLosFallos() {
        // borrar el historial de una clave deja entrar de nuevo sin esperar la ventana completa
        LoginRateLimiter limiter = new LoginRateLimiter(1, 60_000);
        limiter.registrarFallo("a");
        assertTrue(limiter.segundosRestantes("a") > 0);
        limiter.reiniciar("a");
        assertEquals(0, limiter.segundosRestantes("a"));
    }
}
