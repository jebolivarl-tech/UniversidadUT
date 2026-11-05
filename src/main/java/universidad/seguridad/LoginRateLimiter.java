package universidad.seguridad;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Cuenta los intentos de inicio de sesión que fallan y corta el acceso un rato cuando se pasan de un
 * número. La web usa dos contadores con esta misma clase: uno por IP y usuario juntos, que bloquea a los
 * 5 fallos, y otro solo por IP, que bloquea a los 30; los dos dentro de una ventana de quince minutos.
 * Así, quien ataque una cuenta concreta no agota el cupo de las demás, y quien dispare desde una IP contra
 * muchos nombres también se frena.
 *
 * Vale la pena porque comprobar una contraseña con PBKDF2 cuesta, y sin este freno bastaría con dejar un
 * programa probando claves toda la noche. El conteo vive solo en la memoria de esta copia de la aplicación:
 * al reiniciar el servidor la lista se borra, porque no castiga a la persona sino el intento que está
 * ocurriendo ahora.
 */
public final class LoginRateLimiter {

    // Techo de entradas guardadas. Si alguien dispara contra miles de nombres falsos, se limpian las
    // ventanas ya vencidas para que la memoria no crezca sin límite.
    private static final int LIMITE_ENTRADAS = 10_000;

    /** Cuántos fallos lleva una clave y desde qué momento corre su ventana de tiempo. */
    private static final class Registro {
        int fallos;
        long inicioVentana;
    }

    private final int maxFallos;
    private final long ventanaMs;
    // De dónde se toma la hora actual; se puede cambiar para probar sin esperar.
    private final LongSupplier reloj;
    private final Map<String, Registro> registros = new HashMap<>();

    // La web usa este, con el reloj del sistema. El de abajo deja poner un reloj falso en las pruebas.
    public LoginRateLimiter(int maxFallos, long ventanaMs) {
        this(maxFallos, ventanaMs, System::currentTimeMillis);
    }

    public LoginRateLimiter(int maxFallos, long ventanaMs, LongSupplier reloj) {
        this.maxFallos = maxFallos;
        this.ventanaMs = ventanaMs;
        this.reloj = reloj;
    }

    // Las tres operaciones siguientes van protegidas: la lista es una sola para todas las peticiones que
    // llegan a la vez, y sin esa protección dos fallos simultáneos podrían contar uno solo.
    /**
     * Cuántos segundos faltan para poder reintentar, o 0 si la clave no está bloqueada. Si la ventana ya
     * venció, se borra el registro y la cuenta vuelve a empezar de cero.
     */
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
        // Nunca devuelve 0 a alguien que sí está bloqueado: redondea hacia arriba para no prometer un
        // desbloqueo instantáneo.
        return Math.max(1, (ventanaMs - transcurrido + 999) / 1000);
    }

    /** Suma un fallo. Si la ventana anterior ya caducó, abre una nueva en lugar de acumular de viejo. */
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

    /** Un inicio de sesión correcto limpia el historial de esa clave. */
    public synchronized void reiniciar(String clave) {
        registros.remove(clave);
    }
}
