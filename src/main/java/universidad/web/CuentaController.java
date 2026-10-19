package universidad.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.Map;
import universidad.dao.UsuarioDAO;
import universidad.modelo.Cuenta;

/**
 * Gestión de cuentas de acceso (solo ADMINISTRATIVO). La contraseña se recibe por JSON, se guarda
 * hasheada y nunca se devuelve en las respuestas.
 */
public class CuentaController {

    private static final String CONFLICTO = "El nombre de usuario ya existe o la ficha vinculada no está disponible.";

    private final UsuarioDAO dao;

    public CuentaController(UsuarioDAO dao) {
        this.dao = dao;
    }

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
            if (dao.existeUsername(nueva.getUsername())) {
                throw new ApiException(409, CONFLICTO);
            }
            try {
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
            if (esMiCuenta(ctx, id) && !cambios.isEstado()) {
                throw new ApiException(400, "No puede desactivar su propia cuenta.");
            }
            cambios.setIdUsuario(id);
            try {
                dao.actualizar(cambios);
                if (cambios.getPassword() != null && !cambios.getPassword().isBlank()) {
                    dao.cambiarPassword(id, cambios.getPassword());
                }
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible actualizar la cuenta.");
            }
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

    private boolean esMiCuenta(Context ctx, int id) {
        Integer mia = Sesion.idUsuario(ctx);
        return mia != null && mia == id;
    }

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

    private static ApiException traducir(SQLException ex, String mensajeGenerico) {
        if (Http.esReferenciaInexistente(ex)) {
            return new ApiException(400, "La ficha vinculada no existe: revise el id de estudiante o de profesor.");
        }
        return Http.esConflictoDeIntegridad(ex)
                ? new ApiException(409, CONFLICTO)
                : new ApiException(500, mensajeGenerico);
    }
}
