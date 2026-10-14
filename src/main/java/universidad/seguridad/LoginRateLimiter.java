package universidad.seguridad;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Limita intentos fallidos de login por clave (p. ej. IP + usuario) dentro de una ventana de tiempo. */
public final class LoginRateLimiter {

    private static final int LIMITE_ENTRADAS = 10_000;

    private static final class Registro {
        int fallos;
        long inicioVentana;
    }

    private final int maxFallos;
    private final long ventanaMs;
    private final LongSupplier reloj;
    private final Map<String, Registro> registros = new HashMap<>();

    public LoginRateLimiter(int maxFallos, long ventanaMs) {
        this(maxFallos, ventanaMs, System::currentTimeMillis);
    }

    public LoginRateLimiter(int maxFallos, long ventanaMs, LongSupplier reloj) {
        this.maxFallos = maxFallos;
        this.ventanaMs = ventanaMs;
        this.reloj = reloj;
    }

    /** Segundos que faltan para poder reintentar, o 0 si la clave no está bloqueada. */
    public synchronized long segundosRestantes(String clave) {
        Registro r = registros.get(clave);
        if (r == null) {
            return 0;
        }
        long transcurrido = reloj.getAsLong() - r.inicioVentana;
        if (transcurrido >= ventanaMs) {
            registros.remove(clave);
            return 0;
        }
        if (r.fallos < maxFallos) {
            return 0;
        }
        return Math.max(1, (ventanaMs - transcurrido + 999) / 1000);
    }

    public synchronized void registrarFallo(String clave) {
        long ahora = reloj.getAsLong();
        if (registros.size() >= LIMITE_ENTRADAS) {
            registros.values().removeIf(r -> ahora - r.inicioVentana >= ventanaMs);
        }
        Registro r = registros.get(clave);
        if (r == null || ahora - r.inicioVentana >= ventanaMs) {
            r = new Registro();
            r.inicioVentana = ahora;
            registros.put(clave, r);
        }
        r.fallos++;
    }

    public synchronized void reiniciar(String clave) {
        registros.remove(clave);
    }
}
