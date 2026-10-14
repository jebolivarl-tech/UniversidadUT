package universidad.web;

import io.javalin.Javalin;
import java.sql.SQLException;
import java.util.Map;
import universidad.dao.EstudianteDAO;
import universidad.modelo.Estudiante;

public class EstudianteController {

    private final EstudianteDAO dao;

    public EstudianteController(EstudianteDAO dao) {
        this.dao = dao;
    }

    public void registrar(Javalin app) {
        app.get("/api/estudiantes", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO, Sesion.PROFESOR);
            ctx.json(dao.listarTodos());
        });

        app.post("/api/estudiantes", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            Estudiante nuevo = Http.leerJson(ctx, Estudiante.class);
            validar(nuevo);
            try {
                if (dao.registrar(nuevo) == 0) {
                    throw new ApiException(500, "No fue posible registrar el estudiante.");
                }
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible registrar el estudiante.");
            }
            ctx.status(201).json(nuevo);
        });

        app.put("/api/estudiantes/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            int id = Http.leerId(ctx, "id");
            Estudiante estudiante = Http.leerJson(ctx, Estudiante.class);
            validar(estudiante);
            estudiante.setIdEstudiante(id);
            if (dao.leerPorId(id) == null) {
                throw new ApiException(404, "El estudiante no existe.");
            }
            try {
                dao.actualizar(estudiante);
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible actualizar el estudiante.");
            }
            ctx.json(estudiante);
        });

        app.delete("/api/estudiantes/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            int id = Http.leerId(ctx, "id");
            try {
                if (dao.eliminar(id) == 0) {
                    throw new ApiException(404, "El estudiante no existe.");
                }
            } catch (SQLException ex) {
                throw new ApiException(Http.esConflictoDeIntegridad(ex) ? 409 : 500,
                        Http.esConflictoDeIntegridad(ex)
                                ? "No se puede eliminar el estudiante por una restricción de datos."
                                : "No fue posible eliminar el estudiante.");
            }
            ctx.json(Map.of("idEstudiante", id, "eliminado", true));
        });
    }

    private static void validar(Estudiante e) {
        if (!Validaciones.esEstudianteValido(e)) {
            throw new ApiException(400, "Complete todos los campos del estudiante con valores válidos.");
        }
    }

    private static ApiException traducir(SQLException ex, String mensajeGenerico) {
        if (Http.esConflictoDeIntegridad(ex)) {
            return new ApiException(409, "Los datos entran en conflicto con un registro existente.");
        }
        return new ApiException(500, mensajeGenerico);
    }
}
