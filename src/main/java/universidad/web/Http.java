package universidad.web;

import io.javalin.http.Context;
import java.sql.SQLException;
import universidad.conexion.Config;

/**
 * Ayudas sueltas que repiten todos los controladores de la API REST (las rutas HTTP que llama el
 * navegador). HTTP es el protocolo por el que el navegador pide algo y el servidor contesta, y cada
 * respuesta lleva un número de estado: 200 bien, 201 creado, 400 mal formado, 409 conflicto, 500 fallo
 * propio. Aquí se lee el JSON (el texto con llaves que viaja en el cuerpo de la petición), se saca un
 * número de la dirección y se reconocen los códigos de error que manda MySQL.
 */
public final class Http {

    private Http() {
    }

    /**
     * Convierte el cuerpo JSON de la petición en un objeto de la clase que se pida. Primero comprueba el
     * aviso de formato: si no dice application/json se responde 415 en vez de intentar adivinar. Si el
     * JSON está roto o los campos no encajan, 400 con un texto corto, sin repetir el contenido recibido.
     */
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

    /** El navegador manda un número en la dirección (/api/carreras/7); aquí se convierte a entero. */
    public static int leerId(Context ctx, String nombre) {
        try {
            int id = Integer.parseInt(ctx.pathParam(nombre));
            if (id < 1) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException ex) {
            // Cualquiera puede escribir en la dirección, así que un id raro se contesta enseguida y no
            // se llega a armar la consulta.
            throw new ApiException(400, "El ID no es válido.");
        }
    }

    /** MySQL avisa con un estado que empieza por 23 cuando la fila incumple una regla de datos (clave ajena o valor repetido). */
    public static boolean esConflictoDeIntegridad(SQLException ex) {
        return ex.getSQLState() != null && ex.getSQLState().startsWith("23");
    }

    /** 1062: el valor choca con una clave única, o sea que ya está registrado. */
    public static boolean esDuplicado(SQLException ex) {
        return ex.getErrorCode() == 1062;
    }

    /** 1452/1586: se apunta a un id de otra tabla que no existe, por ejemplo una facultad borrada. */
    public static boolean esReferenciaInexistente(SQLException ex) {
        int code = ex.getErrorCode();
        return code == 1452 || code == 1586;
    }

    /** 1451: todavía hay filas apuntando a esta, así que la base de datos no deja borrar. */
    public static boolean esBorradoBloqueado(SQLException ex) {
        return ex.getErrorCode() == 1451;
    }

    /**
     * Dirección IP de la máquina que llama, para contar los intentos de login. La cabecera
     * X-Forwarded-For la escribe quien llama, así que fiarse siempre de ella permitiría fingir otra
     * dirección y saltarse el bloqueo. Solo se usa si la app va detrás de un proxy propio y eso se
     * activó con TRUST_PROXY, que es cuando la dirección real llega en esa cabecera.
     */
    public static String ipCliente(Context ctx) {
        if (universidad.conexion.Config.esVerdadero("TRUST_PROXY")) {
            String reenviada = ctx.header("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                return reenviada.split(",")[0].trim();
            }
        }
        return ctx.ip();
    }

    /** Número que va en la dirección después del signo de pregunta (?curso=3). Null si no viene; 400 si llega escrito de otra forma. */
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

    /** Fecha escrita como AAAA-MM-DD en los parámetros de la dirección; null si no viene, 400 si el texto no es una fecha. */
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
