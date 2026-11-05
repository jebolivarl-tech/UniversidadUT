package universidad;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.session.SessionHandler;
import universidad.conexion.Config;
import universidad.dao.CarreraDAOImpl;
import universidad.dao.EstudianteDAOImpl;
import universidad.dao.UsuarioDAOImpl;
import universidad.servicio.ReglaVioladaException;
import universidad.web.ApiException;
import universidad.web.AuthController;
import universidad.web.CarreraController;
import universidad.web.CuentaController;
import universidad.web.EstudianteController;
import universidad.web.ModulosCrud;
import universidad.web.Recursos;
import universidad.web.Sesion;

/**
 * Prende el servidor web (Javalin) y va anotando, una por una, las páginas HTML que se pueden
 * pedir y la API REST, o sea las rutas /api/... que contestan con JSON en lugar de una página.
 * Todo eso viaja por HTTP, el protocolo con el que el navegador manda pedidos y recibe respuestas.
 * Cada controlador recibe su DAO al registrar sus rutas: el DAO es la pieza que lee y guarda los
 * datos en MySQL, la base de datos del proyecto, escribiendo SQL por JDBC, la forma estándar de
 * hablar con una base de datos desde Java.
 *
 * Lo que se puede cambiar sin recompilar se lee con universidad.conexion.Config: PORT,
 * COOKIE_SECURE (se activa solo cuando el tráfico llega cifrado por HTTPS), CORS_ORIGINS (lista
 * separada por comas; vacía significa que solo se atiende el mismo origen) y las variables de la
 * base de datos.
 */
public class ControladorWeb {

    // La sesión, el recuerdo de que esta persona ya entró, se cierra sola a los 30 minutos sin uso:
    // así un navegador dejado abierto no queda habilitado todo el día.
    private static final int SESION_INACTIVIDAD_SEGUNDOS = 30 * 60;

    public static void main(String[] args) {
        List<String> origenesPermitidos = Config.origenesCors();

        Javalin app = Javalin.create(config -> {
            // Convierte los objetos de Java en JSON y al revés; lo hace con nuestras fechas en formato AAAA-MM-DD.
            config.jsonMapper(new universidad.web.MapeadorJson());
            config.jetty.modifyServletContextHandler(handler -> {
                SessionHandler sesiones = new SessionHandler();
                // Para que un script ajeno no pueda leer la cookie y hacerse pasar por quien ya entró.
                sesiones.setHttpOnly(true);
                // LAX: la cookie no viaja cuando otra página carga algo nuestro de fondo, pero sí cuando la persona llega al sitio.
                sesiones.setSameSite(HttpCookie.SameSite.LAX);
                sesiones.setMaxInactiveInterval(SESION_INACTIVIDAD_SEGUNDOS);
                // La cookie que identifica la sesión solo debe viajar cifrada; en localhost sin HTTPS va en claro y hay que declararlo a mano.
                sesiones.getSessionCookieConfig().setSecure(Config.esVerdadero("COOKIE_SECURE"));
                handler.setSessionHandler(sesiones);
            });
        });

        // Estos dos se ejecutan con cada pedido, antes y después de la ruta que lo atiende.
        app.before(ctx -> aplicarCabecerasDeSeguridad(ctx, origenesPermitidos));
        app.after(ctx -> declararUtf8(ctx));
        // El navegador manda un pedido de tipo OPTIONS antes de uno que modifica datos, para preguntar si
        // lo puede hacer. Aquí se contesta permiso o rechazo, sin llegar a la ruta de verdad.
        app.options("/*", ctx -> {
            if (!esOrigenPermitido(ctx.header("Origin"), origenesPermitidos)) {
                throw new ApiException(403, "Origen no permitido.");
            }
            ctx.status(204);
        });

        registrarManejoDeErrores(app);
        registrarPaginas(app);

        // Cada controlador registra sus rutas y trabaja con el DAO que recibe por parámetro, sin crearlo él por dentro.
        // Login y cuentas usan la misma instancia, porque las dos hablan con la misma tabla de usuarios.
        var usuarios = new UsuarioDAOImpl();
        new AuthController(usuarios).registrar(app);
        new EstudianteController(new EstudianteDAOImpl()).registrar(app);
        new CarreraController(new CarreraDAOImpl()).registrar(app);
        new CuentaController(usuarios).registrar(app);

        ModulosCrud modulos = new ModulosCrud();
        modulos.registrar(app);
        modulos.controladorMiInformacion().registrar(app);

        int port = Config.puerto();
        // Se avisa al apagar el servidor para devolver al pool las conexiones a MySQL que queden abiertas.
        Runtime.getRuntime().addShutdownHook(new Thread(universidad.conexion.Conexion::cerrar));
        app.start(port);
        System.out.println(">>> SERVIDOR ACTIVO EN http://localhost:" + port + " <<<");
    }

    /**
     * Anota las rutas que devuelven páginas y archivos de la interfaz. Nada de esto está suelto en el
     * disco: se lee de la carpeta public que va dentro del .jar. Las páginas privadas declaran una por
     * una qué roles pueden abrirlas.
     */
    private static void registrarPaginas(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/login.html"));
        app.get("/login.html", ctx -> ctx.html(Recursos.leer("public/login.html")));
        app.get("/marca.css", ctx -> servirTexto(ctx, "text/css", "public/marca.css"));
        app.get("/menu.css", ctx -> servirTexto(ctx, "text/css", "public/menu.css"));
        app.get("/modulos.css", ctx -> servirTexto(ctx, "text/css", "public/modulos.css"));
        app.get("/script.js", ctx -> servirTexto(ctx, "application/javascript", "public/script.js"));
        app.get("/modulos.js", ctx -> servirTexto(ctx, "application/javascript", "public/modulos.js"));
        app.get("/logo-ut.png", ctx -> servirBinario(ctx, "image/png", "public/logo-ut.png"));
        app.get("/logo-marca-ut.png", ctx -> servirBinario(ctx, "image/png", "public/logo-marca-ut.png"));

        app.get("/menu.html", ctx -> servirPaginaPrivada(ctx, "public/menu.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR, Sesion.ESTUDIANTE));
        app.get("/mi.html", ctx -> servirPaginaPrivada(ctx, "public/mi.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR, Sesion.ESTUDIANTE));

        app.get("/estudiantes.html", ctx -> servirPaginaPrivada(ctx, "public/estudiantes.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
        app.get("/carreras.html", ctx -> servirPaginaPrivada(ctx, "public/carreras.html", Sesion.ADMINISTRATIVO));
        app.get("/facultades.html", ctx -> servirPaginaPrivada(ctx, "public/facultades.html", Sesion.ADMINISTRATIVO));
        app.get("/profesores.html", ctx -> servirPaginaPrivada(ctx, "public/profesores.html", Sesion.ADMINISTRATIVO));
        app.get("/periodos.html", ctx -> servirPaginaPrivada(ctx, "public/periodos.html", Sesion.ADMINISTRATIVO));
        app.get("/cursos.html", ctx -> servirPaginaPrivada(ctx, "public/cursos.html", Sesion.ADMINISTRATIVO));
        app.get("/usuarios.html", ctx -> servirPaginaPrivada(ctx, "public/usuarios.html", Sesion.ADMINISTRATIVO));

        app.get("/secciones.html", ctx -> servirPaginaPrivada(ctx, "public/secciones.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
        app.get("/matriculas.html", ctx -> servirPaginaPrivada(ctx, "public/matriculas.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
        app.get("/asistencias.html", ctx -> servirPaginaPrivada(ctx, "public/asistencias.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
        app.get("/calificaciones.html", ctx -> servirPaginaPrivada(ctx, "public/calificaciones.html", Sesion.ADMINISTRATIVO, Sesion.PROFESOR));
    }

    /**
     * Convierte cualquier excepción en una respuesta con su código y un JSON con la llave "error",
     * para que el navegador muestre un mensaje entendible en vez de una página de fallo de Java.
     */
    private static void registrarManejoDeErrores(Javalin app) {
        app.exception(ApiException.class, (ex, ctx) ->
                ctx.status(ex.getStatus()).json(Map.of("error", ex.getMessage())));
        // Regla académica que no deja seguir (sección llena, periodo cerrado): la misma excepción trae el
        // código, 409 cuando la regla estorba y 400 cuando el registro señalado no existe.
        app.exception(universidad.servicio.ReglaVioladaException.class, (ex, ctx) ->
                ctx.status(ex.getHttpStatus()).json(Map.of("error", ex.getMessage())));
        // Cuando MySQL no responde, los DAO envuelven el fallo en IllegalStateException. No es culpa del
        // pedido, así que se avisa con 503 y la traza se queda en la consola, no en la respuesta.
        app.exception(IllegalStateException.class, (ex, ctx) -> {
            ex.printStackTrace();
            ctx.status(503).json(Map.of("error", "Servicio no disponible. Compruebe la conexión con la base de datos."));
        });
        // Los errores que arma el propio Javalin, como el 404 de una ruta que no existe, conservan su código.
        app.exception(HttpResponseException.class, (ex, ctx) ->
                ctx.status(ex.getStatus()).json(Map.of("error", ex.getMessage())));
        app.exception(Exception.class, (ex, ctx) -> {
            ex.printStackTrace();
            // Cualquier otro fallo inesperado: mensaje corto y parejo, para no contar detalles internos a quien pregunta.
            ctx.status(500).json(Map.of("error", "Error interno del servidor."));
        });
    }

    /**
     * Entrega un archivo de texto que va dentro del .jar. El charset, o sea cómo se traducen los bytes
     * en letras, se declara ANTES de entregar el contenido: Javalin convierte el texto con el charset
     * que ya esté puesto en la respuesta, y si no hay ninguno Jetty usa ISO-8859-1 y tildes y eñes salen mal.
     */
    private static void servirTexto(Context ctx, String tipo, String recurso) {
        ctx.contentType(tipo + "; charset=utf-8").result(Recursos.leer(recurso));
    }

    // Para los logos no hay charset que valga: viajan como bytes crudos.
    private static void servirBinario(Context ctx, String tipo, String recurso) {
        ctx.contentType(tipo).result(Recursos.leerBytes(recurso));
    }

    /**
     * Entrega una página interna. Sin sesión activa se va al login; con sesión pero con un rol que no
     * puede verla, se va al menú. La comprobación vive en un solo método en vez de repetirse ruta por ruta.
     */
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

    /**
     * Se asegura de que todo cuerpo de texto anuncie UTF-8. Los archivos ya se leen en UTF-8, pero
     * Javalin contesta con "text/html" o "application/json" sin decir el charset; sin esa cabecera el
     * navegador puede tomar la codificación del sistema operativo y las tildes se ven rotas. Se lee la
     * respuesta ya armada en vez de usar ctx.contentType(), porque ese método devuelve el Content-Type
     * del pedido que entró, no el de la salida.
     */
    static void declararUtf8(Context ctx) {
        String tipo = ctx.res().getContentType();
        if (tipo == null) {
            return;
        }
        String base = tipo.toLowerCase();
        boolean esTexto = base.startsWith("text/") || base.startsWith("application/json")
                || base.startsWith("application/javascript");
        if (esTexto && !base.contains("charset")) {
            ctx.contentType(tipo + "; charset=utf-8");
        }
    }

    /**
     * Pone las cabeceras de defensa antes de que la respuesta salga.
     *
     * Las tres fijas dicen: no adivices el tipo de contenido y lo ejecutes como script (nosniff), no
     * me embebas en un marco de otra página (X-Frame-Options) y no cuentes desde qué dirección se
     * llegó (Referrer-Policy).
     *
     * Las de CORS, el permiso para que una página montada en otra dirección pueda leer lo que
     * contestamos, solo se mandan si quien pregunta está en la lista permitida. Se devuelve su propia
     * dirección en lugar de un asterisco, porque con credenciales el navegador no acepta el asterisco;
     * Vary: Origin avisa a las cachés que la respuesta cambia según quién pregunte.
     */
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

    /**
     * Con la lista vacía no entra ningún sitio externo. El propio no necesita permiso: los pedidos que
     * vienen de la misma dirección funcionan sin estas cabeceras.
     */
    static boolean esOrigenPermitido(String origin, List<String> permitidos) {
        return origin != null && permitidos.contains(origin);
    }
}
