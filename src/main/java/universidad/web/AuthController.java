package universidad.web;

import io.javalin.Javalin;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import universidad.conexion.Conexion;
import universidad.dao.UsuarioDAO;
import universidad.dto.LoginRequest;
import universidad.modelo.Usuario;
import universidad.seguridad.LoginRateLimiter;

/**
 * Las cuatro rutas de acceso: comprobar que el servidor y la base de datos responden, iniciar sesión,
 * saber quién está en la sesión abierta y cerrarla. La sesión es el registro que queda en el servidor
 * después de un login correcto; la página no guarda la identidad, solo recibe un identificador y lo
 * presenta en cada petición.
 *
 * La contraseña no se compara con lo que está escrito en la tabla: se recalcula el hash (la huella del
 * texto, que no se puede invertir) y se compara esa huella con la guardada. Toda la comprobación se
 * hace aquí, en el servidor: una página puede ocultar un botón, pero el navegador manda lo que quiera,
 * así que fiarse de la página dejaría entrar a cualquiera.
 */
public class AuthController {

    /** Dentro de esta ventana se cuentan los intentos fallidos: quince minutos. */
    private static final long VENTANA_MS = 15 * 60 * 1000L;

    private final UsuarioDAO usuarioDAO;
    /** Máximo cinco intentos fallidos por cuenta en la ventana. */
    private final LoginRateLimiter porCuenta = new LoginRateLimiter(5, VENTANA_MS);
    /** Y treinta por dirección IP (el número que identifica la máquina que llama), para que nadie pruebe muchas cuentas seguidas desde un mismo equipo. */
    private final LoginRateLimiter porIp = new LoginRateLimiter(30, VENTANA_MS);

    public AuthController(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /** Monta las rutas de acceso en la aplicación. */
    public void registrar(Javalin app) {
        // Control de salud: pide una conexión al pool de conexiones (el grupo de conexiones a MySQL
        // que se reutilizan en vez de abrirla y cerrarla cada vez). Si la consigue, todo arriba; si la
        // base de datos no responde, 503 para que quien supervise sepa que ahora mismo no sirve.
        app.get("/api/health", ctx -> {
            try (Connection ignored = Conexion.getConexion()) {
                ctx.json(Map.of("status", "UP", "database", "UP"));
            } catch (SQLException | IllegalStateException ex) {
                ctx.status(503).json(Map.of("status", "DOWN", "database", "DOWN"));
            }
        });

        app.post("/api/login", ctx -> {
            // El cuerpo de la petición llega en JSON y aquí se convierte en un objeto con usuario y contraseña.
            LoginRequest datos = Http.leerJson(ctx, LoginRequest.class);
            String username = datos.getUsername() == null ? "" : datos.getUsername().trim();
            String password = datos.getPassword() == null ? "" : datos.getPassword();
            if (username.isEmpty() || password.isEmpty()) {
                throw new ApiException(400, "Ingrese usuario y contraseña.");
            }
            // Los límites de longitud evitan cuerpos enormes y cadenas que ni cabrían en la tabla.
            if (username.length() > 50 || password.length() > 200) {
                throw new ApiException(400, "Usuario o contraseña con longitud no válida.");
            }

            // Antes de consultar nada se mira el conteo de fallos. Si la cuenta o la máquina de origen
            // ya fallaron demasiadas veces, se responde 429 y la cabecera Retry-After dice cuántos
            // segundos hay que esperar. Esto protege la contraseña: sin límite, un programa podría
            // probar miles de combinaciones contra una cuenta sin costo.
            String ip = Http.ipCliente(ctx);
            String claveCuenta = ip + "|" + username.toLowerCase();
            long espera = Math.max(porCuenta.segundosRestantes(claveCuenta), porIp.segundosRestantes(ip));
            if (espera > 0) {
                ctx.header("Retry-After", String.valueOf(espera));
                throw new ApiException(429, "Demasiados intentos fallidos. Intente de nuevo más tarde.");
            }

            // El DAO de usuarios busca la fila, comprueba el hash y devuelve null si algo no cuadra
            // (contraseña errada o cuenta desactivada).
            Usuario usuario = usuarioDAO.login(username, password);
            if (usuario == null) {
                porCuenta.registrarFallo(claveCuenta);
                porIp.registrarFallo(ip);
                // El mensaje no distingue si falló el usuario o la contraseña: no se le regala al que
                // prueba combinaciones la confirmación de que la cuenta existe.
                throw new ApiException(401, "Usuario o contraseña incorrectos, o cuenta inactiva.");
            }

            // Acertar limpia el conteo de la cuenta. Luego se abre la sesión en el servidor y se
            // responden solo usuario y rol: la contraseña no viaja de vuelta en el JSON.
            porCuenta.reiniciar(claveCuenta);
            Sesion.iniciar(ctx, usuario);
            ctx.json(Map.of(
                    "authenticated", true,
                    "username", usuario.getUsername(),
                    "rol", usuario.getRol()));
        });

        // Con la sesión abierta, la página pregunta quién es; sin sesión, el propio control de sesión
        // responde 401 y el navegador redirige al login.
        app.get("/api/me", ctx -> {
            Sesion.requerirAutenticacion(ctx);
            ctx.json(Map.of("username", Sesion.username(ctx), "rol", Sesion.rol(ctx)));
        });

        // Cerrar la sesión borra el registro del servidor, no solo el menú de la página. 204 avisa de
        // que se hizo sin necesidad de devolver contenido.
        app.post("/api/logout", ctx -> {
            Sesion.cerrar(ctx);
            ctx.status(204);
        });
    }
}
