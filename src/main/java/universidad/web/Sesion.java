package universidad.web;

import io.javalin.http.Context;
import java.util.Arrays;
import universidad.modelo.Usuario;

/** Manejo de sesión y autorización por rol (se valida siempre en el servidor). */
public final class Sesion {

    public static final String ADMINISTRATIVO = "ADMINISTRATIVO";
    public static final String PROFESOR = "PROFESOR";
    public static final String ESTUDIANTE = "ESTUDIANTE";

    static final String ATTR_AUTENTICADO = "authenticated";
    static final String ATTR_ROL = "rol";
    static final String ATTR_USERNAME = "username";
    static final String ATTR_ID_USUARIO = "idUsuario";
    static final String ATTR_ID_ESTUDIANTE = "idEstudiante";
    static final String ATTR_ID_PROFESOR = "idProfesor";

    private Sesion() {
    }

    /** Crea una sesión nueva (la anterior se invalida para evitar fijación de sesión). */
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

    /** Id de estudiante de la sesión; 403 si el rol no está vinculado a una ficha de estudiante. */
    public static int requerirIdEstudiante(Context ctx) {
        Integer id = idEstudiante(ctx);
        if (id == null || id < 1) {
            throw new ApiException(403, "La cuenta no está vinculada a un estudiante.");
        }
        return id;
    }

    /** Id de profesor de la sesión; 403 si la cuenta no tiene ficha de profesor. */
    public static int requerirIdProfesor(Context ctx) {
        Integer id = idProfesor(ctx);
        if (id == null || id < 1) {
            throw new ApiException(403, "La cuenta no está vinculada a un profesor.");
        }
        return id;
    }

    /** 401 si no hay sesión. */
    public static void requerirAutenticacion(Context ctx) {
        if (!estaAutenticado(ctx)) {
            throw new ApiException(401, "Debe iniciar sesión para acceder a este recurso.");
        }
    }

    /** 401 si no hay sesión y 403 si el rol de la sesión no está entre los permitidos. */
    public static void requerirRol(Context ctx, String... rolesPermitidos) {
        requerirAutenticacion(ctx);
        String rolActual = rol(ctx);
        if (rolActual == null || !Arrays.asList(rolesPermitidos).contains(rolActual)) {
            throw new ApiException(403, "No tiene permisos para realizar esta operación.");
        }
    }
}
