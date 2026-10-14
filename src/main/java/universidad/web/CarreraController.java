package universidad.web;

import io.javalin.Javalin;
import java.sql.SQLException;
import universidad.dao.CarreraDAO;
import universidad.modelo.Carrera;

public class CarreraController {

    private final CarreraDAO dao;

    public CarreraController(CarreraDAO dao) {
        this.dao = dao;
    }

    public void registrar(Javalin app) {
        app.get("/api/carreras", ctx -> {
            Sesion.requerirAutenticacion(ctx);
            ctx.json(dao.listarTodos());
        });

        app.post("/api/carreras", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            Carrera carrera = Http.leerJson(ctx, Carrera.class);
            validar(carrera);
            try {
                if (dao.registrar(carrera) == 0) {
                    throw new ApiException(500, "No fue posible registrar la carrera.");
                }
            } catch (SQLException ex) {
                throw new ApiException(Http.esConflictoDeIntegridad(ex) ? 409 : 500,
                        Http.esConflictoDeIntegridad(ex)
                                ? "La facultad indicada no existe o los datos entran en conflicto."
                                : "No fue posible registrar la carrera.");
            }
            ctx.status(201).json(carrera);
        });

        app.put("/api/carreras/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            int id = Http.leerId(ctx, "id");
            Carrera carrera = Http.leerJson(ctx, Carrera.class);
            validar(carrera);
            carrera.setIdCarrera(id);
            try {
                if (dao.leerPorId(id) == null) {
                    throw new ApiException(404, "La carrera no existe.");
                }
                dao.actualizar(carrera);
            } catch (SQLException ex) {
                throw new ApiException(Http.esConflictoDeIntegridad(ex) ? 409 : 500,
                        Http.esConflictoDeIntegridad(ex)
                                ? "La facultad indicada no existe o los datos entran en conflicto."
                                : "No fue posible actualizar la carrera.");
            }
            ctx.status(204);
        });

        app.delete("/api/carreras/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            int id = Http.leerId(ctx, "id");
            try {
                if (dao.eliminar(id) == 0) {
                    throw new ApiException(404, "La carrera no existe.");
                }
            } catch (SQLException ex) {
                if (Http.esConflictoDeIntegridad(ex)) {
                    throw new ApiException(409, "No se puede eliminar una carrera que tiene estudiantes asociados.");
                }
                throw new ApiException(500, "No fue posible eliminar la carrera.");
            }
            ctx.status(204);
        });
    }

    private static void validar(Carrera c) {
        if (!Validaciones.esCarreraValida(c)) {
            throw new ApiException(400, "Ingrese un nombre y un ID de facultad válido.");
        }
    }
}
