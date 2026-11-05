package universidad.web;

import io.javalin.http.Context;
import java.util.Arrays;
import universidad.modelo.Usuario;

/**
 * Qué es la sesión y quién puede hacer qué. Una sesión es la ficha que el servidor guarda tras un login
 * correcto: aquí se anotan el rol, el usuario y las fichas de estudiante o profesor vinculadas. El
 * navegador recibe solo un identificador de esa sesión y lo manda en cada petición HTTP.
 *
 * Por eso el permiso se decide aquí y no en la página: la página la controla el usuario del navegador,
 * que puede ocultar un botón pero también llamar la ruta a mano. Lo que no puede fingir es la sesión,
 * porque esa vive en el servidor.
 */
public final class Sesion {

    public static final String ADMINISTRATIVO = "ADMINISTRATIVO";
    public static final String PROFESOR = "PROFESOR";
    public static final String ESTUDIANTE = "ESTUDIANTE";

    // Nombres con los que cada dato queda guardado en la sesión; son internos de esta clase.
    static final String ATTR_AUTENTICADO = "authenticated";
    static final String ATTR_ROL = "rol";
    static final String ATTR_USERNAME = "username";
    static final String ATTR_ID_USUARIO = "idUsuario";
    static final String ATTR_ID_ESTUDIANTE = "idEstudiante";
    static final String ATTR_ID_PROFESOR = "idProfesor";

    private Sesion() {
    }

    /**
     * Abre la sesión del usuario que acaba de hacer login y guarda su rol y sus fichas.
     * Primero se invalida la sesión que hubiera: si se siguiera usando una vieja, alguien que conociera
     * ese identificador podría colarse con los datos de otro (a esto le llaman fijación de sesión). Por
     * eso se bota y se crea una nueva de cero, con un identificador que nadie tenía antes.
     */
    public static void iniciar(Context ctx, Usuario usuario) {
        var anterior = ctx.req().getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        ctx.req().getSession(true);
        ctx.sessionAttribute(ATTR_AUTENTICADO, true);
        ctx.sessionAttribute(ATTR_ROL, usuario.getRol());
        ctx.sessionAttribute(ATTR_USERNAME, usuario.getUsername());
        ctx.sessionAttribute(ATTR_ID_USUARIO, usuario.getIdUsuario());
        ctx.sessionAttribute(ATTR_ID_ESTUDIANTE, usuario.getIdEstudiante());
        ctx.sessionAttribute(ATTR_ID_PROFESOR, usuario.getIdProfesor());
    }

    /** Al cerrar se destruye la ficha del servidor: cerrar la pestaña no cierra la sesión. */
    public static void cerrar(Context ctx) {
        var sesion = ctx.req().getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }

    public static boolean estaAutenticado(Context ctx) {
        return Boolean.TRUE.equals(ctx.sessionAttribute(ATTR_AUTENTICADO));
    }

    public static String rol(Context ctx) {
        return ctx.sessionAttribute(ATTR_ROL);
    }

    public static String username(Context ctx) {
        return ctx.sessionAttribute(ATTR_USERNAME);
    }

    public static Integer idUsuario(Context ctx) {
        return ctx.sessionAttribute(ATTR_ID_USUARIO);
    }

    public static Integer idEstudiante(Context ctx) {
        return ctx.sessionAttribute(ATTR_ID_ESTUDIANTE);
    }

    public static Integer idProfesor(Context ctx) {
        return ctx.sessionAttribute(ATTR_ID_PROFESOR);
    }

    /** Id de estudiante guardado en la sesión; 403 si la cuenta no está atada a una ficha de estudiante. */
    public static int requerirIdEstudiante(Context ctx) {
        Integer id = idEstudiante(ctx);
        if (id == null || id < 1) {
            throw new ApiException(403, "La cuenta no está vinculada a un estudiante.");
        }
        return id;
    }

    /** Id de profesor guardado en la sesión; 403 si la cuenta no tiene ficha de profesor. */
    public static int requerirIdProfesor(Context ctx) {
        Integer id = idProfesor(ctx);
        if (id == null || id < 1) {
            throw new ApiException(403, "La cuenta no está vinculada a un profesor.");
        }
        return id;
    }

    /** Puerta de entrada de las rutas privadas: sin sesión en el servidor, 401. */
    public static void requerirAutenticacion(Context ctx) {
        if (!estaAutenticado(ctx)) {
            throw new ApiException(401, "Debe iniciar sesión para acceder a este recurso.");
        }
    }

    /**
     * Comprobación del rol, usada por todas las rutas de la API REST. Primero exige sesión y luego mira
     * el rol apuntado en esa sesión contra la lista de roles permitidos: 401 si nadie entró y 403 si
     * entró pero no le toca. El rol lo puso el login, no el navegador, así que no se puede cambiar.
     */
    public static void requerirRol(Context ctx, String... rolesPermitidos) {
        requerirAutenticacion(ctx);
        String rolActual = rol(ctx);
        if (rolActual == null || !Arrays.asList(rolesPermitidos).contains(rolActual)) {
            throw new ApiException(403, "No tiene permisos para realizar esta operación.");
        }
    }
}
