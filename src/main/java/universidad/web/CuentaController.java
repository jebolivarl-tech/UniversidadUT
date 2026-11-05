package universidad.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.Map;
import universidad.dao.UsuarioDAO;
import universidad.modelo.Cuenta;

/**
 * Altas, cambios y bajas de las cuentas que pueden iniciar sesión (la tabla usuarios). Solo lo hace el
 * rol ADMINISTRATIVO, y se comprueba el rol en la sesión del servidor en cada una de las rutas.
 *
 * La contraseña entra por el JSON de la petición y ahí se queda: al guardarla se convierte a hash (una
 * huella calculada que no se puede devolver a texto), y el objeto {@link universidad.modelo.Cuenta}
 * está marcado para que ese campo se lea de la petición pero nunca se escriba en la respuesta. Así,
 * aunque alguien pida la lista de cuentas, no sale ni la contraseña ni su huella.
 */
public class CuentaController {

    /** Mensaje único para los choques: no se detalla qué dato está ocupado, para no confirmar cuentas ajenas. */
    private static final String CONFLICTO = "El nombre de usuario ya existe o la ficha vinculada no está disponible.";

    private final UsuarioDAO dao;

    public CuentaController(UsuarioDAO dao) {
        this.dao = dao;
    }

    /** Monta las rutas de cuentas en la aplicación. */
    public void registrar(Javalin app) {
        app.get("/api/usuarios", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            ctx.json(dao.listar());
        });

        app.get("/api/usuarios/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            Cuenta cuenta = dao.leerPorId(Http.leerId(ctx, "id"));
            if (cuenta == null) {
                throw new ApiException(404, "No se encontró la cuenta.");
            }
            ctx.json(cuenta);
        });

        app.post("/api/usuarios", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            Cuenta nueva = leerCuenta(ctx, true);
            // Se avisa del nombre repetido antes de intentar insertar, para contestar con un texto claro.
            if (dao.existeUsername(nueva.getUsername())) {
                throw new ApiException(409, CONFLICTO);
            }
            try {
                // El DAO hashea la contraseña al hacer el INSERT; lo que se responde es la cuenta ya
                // guardada, y el campo contraseña no viaja en ese JSON.
                ctx.status(201).json(dao.crear(nueva));
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible crear la cuenta.");
            }
        });

        app.put("/api/usuarios/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            int id = Http.leerId(ctx, "id");
            Cuenta existente = dao.leerPorId(id);
            if (existente == null) {
                throw new ApiException(404, "No se encontró la cuenta.");
            }
            Cuenta cambios = leerCuenta(ctx, false);
            if (!cambios.getUsername().equalsIgnoreCase(existente.getUsername())
                    && dao.existeUsername(cambios.getUsername())) {
                throw new ApiException(409, CONFLICTO);
            }
            // Nadie puede desactivarse a sí mismo ni borrarse: quedaría sin cuenta con rol ADMINISTRATIVO
            // para volver a entrar, y esto se corta en el servidor aunque alguien mande el cambio a mano.
            if (esMiCuenta(ctx, id) && !cambios.isEstado()) {
                throw new ApiException(400, "No puede desactivar su propia cuenta.");
            }
            cambios.setIdUsuario(id);
            try {
                dao.actualizar(cambios);
                // Al editar, la contraseña puede no venir o venir en blanco: significa "no la cambies",
                // así que solo se guarda cuando se escribió una nueva.
                if (cambios.getPassword() != null && !cambios.getPassword().isBlank()) {
                    dao.cambiarPassword(id, cambios.getPassword());
                }
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible actualizar la cuenta.");
            }
            // Se vuelve a leer del DAO en vez de responder el objeto recibido: así la respuesta sale
            // siempre sin contraseña y con los valores ya guardados.
            ctx.json(dao.leerPorId(id));
        });

        app.delete("/api/usuarios/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            int id = Http.leerId(ctx, "id");
            if (esMiCuenta(ctx, id)) {
                throw new ApiException(400, "No puede eliminar su propia cuenta.");
            }
            try {
                if (dao.eliminar(id) == 0) {
                    throw new ApiException(404, "No se encontró la cuenta.");
                }
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible eliminar la cuenta.");
            }
            ctx.json(Map.of("id", id, "eliminado", true));
        });
    }

    /** Compara el id pedido con el id del usuario que está en la sesión. */
    private boolean esMiCuenta(Context ctx, int id) {
        Integer mia = Sesion.idUsuario(ctx);
        return mia != null && mia == id;
    }

    /** Lee la cuenta del JSON, la deja en condiciones (sin espacios, rol en mayúsculas) y la revisa. */
    private Cuenta leerCuenta(Context ctx, boolean esCreacion) {
        Cuenta cuenta = Http.leerJson(ctx, Cuenta.class);
        if (cuenta.getUsername() != null) {
            cuenta.setUsername(cuenta.getUsername().trim());
        }
        if (cuenta.getRol() != null) {
            cuenta.setRol(cuenta.getRol().toUpperCase());
        }
        if (!Validaciones.esCuentaValida(cuenta, esCreacion)) {
            throw new ApiException(400, esCreacion
                    ? "Cuenta inválida: usuario de 3 a 50 caracteres, rol válido, contraseña de al menos 10 caracteres"
                      + " y ficha vinculada cuando el rol es PROFESOR o ESTUDIANTE."
                    : "Cuenta inválida: revise el usuario, el rol y la ficha vinculada.");
        }
        return cuenta;
    }

    /**
     * Igual que en los demás módulos: el número de error de MySQL decide el código HTTP. Ficha
     * inexistente es culpa del dato enviado (400), un usuario repetido es conflicto (409) y lo demás
     * queda en 500 sin enseñar el detalle de la base de datos.
     */
    private static ApiException traducir(SQLException ex, String mensajeGenerico) {
        if (Http.esReferenciaInexistente(ex)) {
            return new ApiException(400, "La ficha vinculada no existe: revise el id de estudiante o de profesor.");
        }
        return Http.esConflictoDeIntegridad(ex)
                ? new ApiException(409, CONFLICTO)
                : new ApiException(500, mensajeGenerico);
    }
}
