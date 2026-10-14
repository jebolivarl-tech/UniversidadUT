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

public class AuthController {

    private static final long VENTANA_MS = 15 * 60 * 1000L;

    private final UsuarioDAO usuarioDao;
    private final LoginRateLimiter porCuenta = new LoginRateLimiter(5, VENTANA_MS);
    private final LoginRateLimiter porIp = new LoginRateLimiter(30, VENTANA_MS);

    public AuthController(UsuarioDAO usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public void registrar(Javalin app) {
        app.get("/api/health", ctx -> {
            try (Connection ignored = Conexion.getConexion()) {
                ctx.json(Map.of("status", "UP", "database", "UP"));
            } catch (SQLException | IllegalStateException ex) {
                ctx.status(503).json(Map.of("status", "DOWN", "database", "DOWN"));
            }
        });

        app.post("/api/login", ctx -> {
            LoginRequest datos = Http.leerJson(ctx, LoginRequest.class);
            String username = datos.getUsername() == null ? "" : datos.getUsername().trim();
            String password = datos.getPassword() == null ? "" : datos.getPassword();
            if (username.isEmpty() || password.isEmpty()) {
                throw new ApiException(400, "Ingrese usuario y contraseña.");
            }
            if (username.length() > 50 || password.length() > 200) {
                throw new ApiException(400, "Usuario o contraseña con longitud no válida.");
            }

            String ip = Http.ipCliente(ctx);
            String claveCuenta = ip + "|" + username.toLowerCase();
            long espera = Math.max(porCuenta.segundosRestantes(claveCuenta), porIp.segundosRestantes(ip));
            if (espera > 0) {
                ctx.header("Retry-After", String.valueOf(espera));
                throw new ApiException(429, "Demasiados intentos fallidos. Intente de nuevo más tarde.");
            }

            Usuario usuario = usuarioDao.login(username, password);
            if (usuario == null) {
                porCuenta.registrarFallo(claveCuenta);
                porIp.registrarFallo(ip);
                throw new ApiException(401, "Usuario o contraseña incorrectos, o cuenta inactiva.");
            }

            porCuenta.reiniciar(claveCuenta);
            Sesion.iniciar(ctx, usuario);
            ctx.json(Map.of(
                    "authenticated", true,
                    "username", usuario.getUsername(),
                    "rol", usuario.getRol()));
        });

        app.get("/api/me", ctx -> {
            Sesion.requerirAutenticacion(ctx);
            ctx.json(Map.of("username", Sesion.username(ctx), "rol", Sesion.rol(ctx)));
        });

        app.post("/api/logout", ctx -> {
            Sesion.cerrar(ctx);
            ctx.status(204);
        });
    }
}
