package universidad.web;

import io.javalin.http.Context;
import java.sql.SQLException;
import universidad.conexion.Config;

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

    /** 1062: valor duplicado para una clave única. */
    public static boolean esDuplicado(SQLException ex) {
        return ex.getErrorCode() == 1062;
    }

    /** 1452/1586: la fila referencia un id que no existe (clave foránea). */
    public static boolean esReferenciaInexistente(SQLException ex) {
        int code = ex.getErrorCode();
        return code == 1452 || code == 1586;
    }

    /** 1451: otra tabla todavía referencia la fila, así que la base de datos bloquea el borrado. */
    public static boolean esBorradoBloqueado(SQLException ex) {
        return ex.getErrorCode() == 1451;
    }

    /** IP del cliente. Solo confía en X-Forwarded-For si TRUST_PROXY=true (detrás de un proxy inverso propio). */
    public static String ipCliente(Context ctx) {
        if (universidad.conexion.Config.esVerdadero("TRUST_PROXY")) {
            String reenviada = ctx.header("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                return reenviada.split(",")[0].trim();
            }
        }
        return ctx.ip();
    }

    /** Entero de la query string; null si no viene. 400 si no es un entero positivo. */
    public static Integer enteroQuery(Context ctx, String nombre) {
        String valor = ctx.queryParam(nombre);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            int numero = Integer.parseInt(valor.trim());
            if (numero < 1) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "El parámetro " + nombre + " no es válido.");
        }
    }

    /** Fecha ISO (yyyy-MM-dd) de la query string; null si no viene. */
    public static java.sql.Date fechaQuery(Context ctx, String nombre) {
        String valor = ctx.queryParam(nombre);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(valor.trim());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "El parámetro " + nombre + " debe ser una fecha AAAA-MM-DD.");
        }
    }
}
