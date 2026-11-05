package universidad.web;

import io.javalin.Javalin;
import java.sql.SQLException;
import universidad.dao.CarreraDAO;
import universidad.modelo.Carrera;

/**
 * Rutas HTTP de la tabla de carreras: listar, crear, modificar y borrar. Es el ejemplo escrito a mano,
 * sin apoyarse en {@link CrudController}, para que se vea cómo es una ruta de la API REST por dentro.
 * Leer carreras lo permiten al administrativo y al profesor; escribir, solo al administrativo. El rol se
 * vuelve a comprobar en cada petición mirando la sesión del servidor, porque ocultar el botón en la
 * página no impide que alguien llame la ruta directamente.
 * El DAO es la clase que escribe y lee esa tabla usando JDBC, la librería de Java para hablar con MySQL.
 */
public class CarreraController {

    private final CarreraDAO dao;

    public CarreraController(CarreraDAO dao) {
        this.dao = dao;
    }

    /** Monta las rutas de carreras en la aplicación. */
    public void registrar(Javalin app) {
        app.get("/api/carreras", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO, Sesion.PROFESOR);
            ctx.json(dao.listarTodos());
        });

        app.post("/api/carreras", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            // El JSON del cuerpo se convierte en el objeto Carrera; el id lo pone la base de datos.
            Carrera carrera = Http.leerJson(ctx, Carrera.class);
            validar(carrera);
            try {
                if (dao.registrar(carrera) == 0) {
                    throw new ApiException(500, "No fue posible registrar la carrera.");
                }
            } catch (SQLException ex) {
                // MySQL rechaza la fila si la facultad no existe o el nombre choca con otro. Ese error
                // de base de datos se traduce aquí a un código HTTP que el navegador entiende: 409 si
                // el problema es del dato enviado, 500 si es del servidor.
                throw new ApiException(Http.esConflictoDeIntegridad(ex) ? 409 : 500,
                        Http.esConflictoDeIntegridad(ex)
                                ? "La facultad indicada no existe o los datos entran en conflicto."
                                : "No fue posible registrar la carrera.");
            }
            // 201: se creó. Se devuelve la carrera ya con el id que asignó la base de datos.
            ctx.status(201).json(carrera);
        });

        app.put("/api/carreras/{id}", ctx -> {
            Sesion.requerirRol(ctx, Sesion.ADMINISTRATIVO);
            // El id llega escrito en la dirección; aquí se convierte a entero y se descarta si no lo es.
            int id = Http.leerId(ctx, "id");
            Carrera carrera = Http.leerJson(ctx, Carrera.class);
            validar(carrera);
            // El id de la ruta manda sobre el que venga en el JSON, para que nadie modifique otra fila.
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
                // Si aún hay estudiantes apuntados a esa carrera, la base de datos bloquea el borrado
                // para no dejar filas sueltas: se avisa con 409 en vez de dejar un 500 incomprensible.
                if (Http.esConflictoDeIntegridad(ex)) {
                    throw new ApiException(409, "No se puede eliminar una carrera que tiene estudiantes asociados.");
                }
                throw new ApiException(500, "No fue posible eliminar la carrera.");
            }
            ctx.status(204);
        });
    }

    /** Rechaza la carrera sin nombre o sin facultad antes de llegar a la base de datos (400). */
    private static void validar(Carrera c) {
        if (!Validaciones.esCarreraValida(c)) {
            throw new ApiException(400, "Ingrese un nombre y un ID de facultad válido.");
        }
    }
}
