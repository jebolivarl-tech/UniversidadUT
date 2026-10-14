package universidad;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.session.SessionHandler;
import universidad.dao.CarreraDAOImpl;
import universidad.dao.EstudianteDAOImpl;
import universidad.dao.UsuarioDAOImpl;
import universidad.web.ApiException;
import universidad.web.AuthController;
import universidad.web.CarreraController;
import universidad.web.EstudianteController;
import universidad.web.Recursos;
import universidad.web.Sesion;

/**
 * Punto de entrada del servidor web (Javalin).
 *
 * Variables de entorno: PORT, DB_URL, DB_USER, DB_PASSWORD, COOKIE_SECURE (true en producción con HTTPS),
 * CORS_ORIGINS (lista separada por comas; vacío = solo mismo origen), TRUST_PROXY (true detrás de un proxy propio).
 */
public class ControladorWeb {

    private static final int SESION_INACTIVIDAD_SEGUNDOS = 30 * 60;

    public static void main(String[] args) {
        List<String> origenesPermitidos = leerOrigenesCors();

        Javalin app = Javalin.create(config ->
                config.jetty.modifyServletContextHandler(handler -> {
                    SessionHandler sesiones = new SessionHandler();
                    sesiones.setHttpOnly(true);
                    sesiones.setSameSite(HttpCookie.SameSite.LAX);
                    sesiones.setMaxInactiveInterval(SESION_INACTIVIDAD_SEGUNDOS);
                    sesiones.getSessionCookieConfig().setSecure("true".equalsIgnoreCase(System.getenv("COOKIE_SECURE")));
                    handler.setSessionHandler(sesiones);
                }));

        app.before(ctx -> aplicarCabecerasDeSeguridad(ctx, origenesPermitidos));
        app.options("/*", ctx -> {
            if (!esOrigenPermitido(ctx.header("Origin"), origenesPermitidos)) {
                throw new ApiException(403, "Origen no permitido.");
            }
            ctx.status(204);
        });

        registrarManejoDeErrores(app);
        registrarPaginas(app);

        new AuthController(new UsuarioDAOImpl()).registrar(app);
        new EstudianteController(new EstudianteDAOImpl()).registrar(app);
        new CarreraController(new CarreraDAOImpl()).registrar(app);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Runtime.getRuntime().addShutdownHook(new Thread(universidad.conexion.Conexion::cerrar));
        app.start(port);
        System.out.println(">>> SERVIDOR ACTIVO EN http://localhost:" + port + " <<<");
    }

    private static void registrarPaginas(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/login.html"));
        app.get("/login.html", ctx -> ctx.html(Recursos.leer("public/login.html")));
        app.get("/menu.css", ctx -> ctx.contentType("text/css").result(Recursos.leer("public/menu.css")));
        app.get("/script.js", ctx -> ctx.contentType("application/javascript").result(Recursos.leer("public/script.js")));
        app.get("/menu.html", ctx -> servirPaginaPrivada(ctx, "public/menu.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR, Sesion.ESTUDIANTE));
        app.get("/estudiantes.html", ctx -> servirPaginaPrivada(ctx, "public/estudiantes.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
        app.get("/carreras.html", ctx -> servirPaginaPrivada(ctx, "public/carreras.html", Sesion.ADMINISTRATIVO));
    }

    private static void registrarManejoDeErrores(Javalin app) {
        app.exception(ApiException.class, (ex, ctx) ->
                ctx.status(ex.getStatus()).json(Map.of("error", ex.getMessage())));
        app.exception(IllegalStateException.class, (ex, ctx) -> {
            ex.printStackTrace();
            ctx.status(503).json(Map.of("error", "Servicio no disponible. Compruebe la conexión con la base de datos."));
        });
        // Errores propios de Javalin (404 de ruta inexistente, 405, etc.) conservan su código.
        app.exception(HttpResponseException.class, (ex, ctx) ->
                ctx.status(ex.getStatus()).json(Map.of("error", ex.getMessage())));
        app.exception(Exception.class, (ex, ctx) -> {
            ex.printStackTrace();
            ctx.status(500).json(Map.of("error", "Error interno del servidor."));
        });
    }

    static void servirPaginaPrivada(Context ctx, String recurso, String... roles) {
        if (!Sesion.estaAutenticado(ctx)) {
            ctx.redirect("/login.html");
            return;
        }
        if (!Arrays.asList(roles).contains(Sesion.rol(ctx))) {
            ctx.redirect("/menu.html");
            return;
        }
        ctx.html(Recursos.leer(recurso));
    }

    private static void aplicarCabecerasDeSeguridad(Context ctx, List<String> origenesPermitidos) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "no-referrer");

        String origin = ctx.header("Origin");
        if (esOrigenPermitido(origin, origenesPermitidos)) {
            ctx.header("Access-Control-Allow-Origin", origin);
            ctx.header("Access-Control-Allow-Credentials", "true");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Accept, Content-Type");
            ctx.header("Access-Control-Max-Age", "3600");
            ctx.header("Vary", "Origin");
        }
    }

    static boolean esOrigenPermitido(String origin, List<String> permitidos) {
        return origin != null && permitidos.contains(origin);
    }

    static List<String> leerOrigenesCors() {
        String valor = System.getenv("CORS_ORIGINS");
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
