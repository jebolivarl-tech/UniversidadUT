package universidad.web;

import io.javalin.http.Context;
import java.sql.SQLException;

/** Utilidades HTTP comunes a todos los controladores. */
public final class Http {

    private Http() {
    }

    public static <T> T leerJson(Context ctx, Class<T> tipo) {
        String contentType = ctx.contentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw new ApiException(415, "El cuerpo debe enviarse como application/json.");
        }
        try {
            T cuerpo = ctx.bodyAsClass(tipo);
            if (cuerpo == null) {
                throw new ApiException(400, "El cuerpo JSON no es válido.");
            }
            return cuerpo;
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(400, "El cuerpo JSON no es válido.");
        }
    }

    public static int leerId(Context ctx, String nombre) {
        try {
            int id = Integer.parseInt(ctx.pathParam(nombre));
            if (id < 1) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "El ID no es válido.");
        }
    }

    public static boolean esConflictoDeIntegridad(SQLException ex) {
        return ex.getSQLState() != null && ex.getSQLState().startsWith("23");
    }

    /** IP del cliente. Solo confía en X-Forwarded-For si TRUST_PROXY=true (detrás de un proxy inverso propio). */
    public static String ipCliente(Context ctx) {
        if ("true".equalsIgnoreCase(System.getenv("TRUST_PROXY"))) {
            String reenviada = ctx.header("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                return reenviada.split(",")[0].trim();
            }
        }
        return ctx.ip();
    }
}
