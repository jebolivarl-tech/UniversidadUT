package universidad.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import universidad.dao.CRUD;

/**
 * Controlador REST genérico (GET lista / GET uno / POST / PUT / DELETE) para las entidades con
 * CRUD simple. Los roles se exigen siempre en el servidor y los extras (cupo, propiedad de la
 * sección, duplicados) se inyectan con {@code antesEscribir}.
 */
public final class CrudController<T> {

    private final CRUD<T> dao;
    private final Class<T> tipo;
    private final String ruta;
    private final String nombreEntidad;

    private Function<Context, List<T>> listado;
    private BiConsumer<T, Integer> fijarId = (t, id) -> { };
    private Predicate<T> valido = t -> true;
    private List<String> rolesLectura = List.of(Sesion.ADMINISTRATIVO, Sesion.PROFESOR);
    private List<String> rolesEscritura = List.of(Sesion.ADMINISTRATIVO);
    private BiConsumer<Context, T> antesEscribir = (ctx, t) -> { };
    private BiConsumer<Context, Integer> antesEliminar = (ctx, id) -> { };
    private Function<T, T> despuesEscribir = t -> t;
    private String mensajeConflicto = "Los datos entran en conflicto con un registro existente.";

    public CrudController(CRUD<T> dao, Class<T> tipo, String ruta, String nombreEntidad) {
        this.dao = dao;
        this.tipo = tipo;
        this.ruta = ruta;
        this.nombreEntidad = nombreEntidad;
        this.listado = ctx -> dao.listarTodos(); // sin filtro definido, la lista completa es la respuesta correcta
    }

    public CrudController<T> listado(Function<Context, List<T>> listado) {
        this.listado = listado;
        return this;
    }

    public CrudController<T> fijarId(BiConsumer<T, Integer> fijarId) {
        this.fijarId = fijarId;
        return this;
    }

    public CrudController<T> valido(Predicate<T> valido) {
        this.valido = valido;
        return this;
    }

    public CrudController<T> rolesLectura(String... roles) {
        this.rolesLectura = List.of(roles);
        return this;
    }

    public CrudController<T> rolesEscritura(String... roles) {
        this.rolesEscritura = List.of(roles);
        return this;
    }

    public CrudController<T> antesEscribir(BiConsumer<Context, T> antesEscribir) {
        this.antesEscribir = antesEscribir;
        return this;
    }

    /** Regla que debe cumplirse antes de borrar (una sección con matriculados no se elimina). */
    public CrudController<T> antesEliminar(BiConsumer<Context, Integer> antesEliminar) {
        this.antesEliminar = antesEliminar;
        return this;
    }

    public CrudController<T> despuesEscribir(Function<T, T> despuesEscribir) {
        this.despuesEscribir = despuesEscribir;
        return this;
    }

    public CrudController<T> mensajeConflicto(String mensajeConflicto) {
        this.mensajeConflicto = mensajeConflicto;
        return this;
    }

    public void registrar(Javalin app) {
        app.get("/api/" + ruta, ctx -> {
            Sesion.requerirRol(ctx, rolesLectura.toArray(new String[0]));
            ctx.json(listado.apply(ctx));
        });

        app.get("/api/" + ruta + "/{id}", ctx -> {
            Sesion.requerirRol(ctx, rolesLectura.toArray(new String[0]));
            T entidad = dao.leerPorId(Http.leerId(ctx, "id"));
            if (entidad == null) {
                throw new ApiException(404, "No se encontró " + nombreEntidad + ".");
            }
            ctx.json(entidad);
        });

        app.post("/api/" + ruta, ctx -> {
            Sesion.requerirRol(ctx, rolesEscritura.toArray(new String[0]));
            T nueva = Http.leerJson(ctx, tipo);
            fijarId.accept(nueva, 0); // un alta nunca trae id: lo asigna la base de datos
            validar(nueva);
            antesEscribir.accept(ctx, nueva);
            try {
                if (dao.registrar(nueva) == 0) {
                    throw new ApiException(500, "No fue posible registrar " + nombreEntidad + ".");
                }
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible registrar " + nombreEntidad + ".");
            }
            ctx.status(201).json(despuesEscribir.apply(nueva));
        });

        app.put("/api/" + ruta + "/{id}", ctx -> {
            Sesion.requerirRol(ctx, rolesEscritura.toArray(new String[0]));
            int id = Http.leerId(ctx, "id");
            if (dao.leerPorId(id) == null) {
                throw new ApiException(404, "No se encontró " + nombreEntidad + ".");
            }
            T entidad = Http.leerJson(ctx, tipo);
            validar(entidad);
            fijarId.accept(entidad, id);
            antesEscribir.accept(ctx, entidad);
            try {
                dao.actualizar(entidad);
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible actualizar " + nombreEntidad + ".");
            }
            ctx.json(despuesEscribir.apply(entidad));
        });

        app.delete("/api/" + ruta + "/{id}", ctx -> {
            Sesion.requerirRol(ctx, rolesEscritura.toArray(new String[0]));
            int id = Http.leerId(ctx, "id");
            antesEliminar.accept(ctx, id);
            try {
                if (dao.eliminar(id) == 0) {
                    throw new ApiException(404, "No se encontró " + nombreEntidad + ".");
                }
            } catch (SQLException ex) {
                throw traducir(ex, "No fue posible eliminar " + nombreEntidad + ".");
            }
            ctx.json(Map.of("id", id, "eliminado", true));
        });
    }

    private void validar(T entidad) {
        if (!valido.test(entidad)) {
            throw new ApiException(400, "Revise los campos de " + nombreEntidad + ": hay datos vacíos o fuera de rango.");
        }
    }

    private ApiException traducir(SQLException ex, String mensajeGenerico) {
        if (Http.esReferenciaInexistente(ex)) {
            return new ApiException(400, "Alguna referencia no existe: revise la facultad, carrera, profesor, curso, periodo o estudiante indicados.");
        }
        if (Http.esBorradoBloqueado(ex)) {
            return new ApiException(409, "No se puede eliminar " + nombreEntidad
                    + ": todavía hay registros matriculados o asociados a él.");
        }
        if (Http.esDuplicado(ex)) {
            return new ApiException(409, mensajeConflicto);
        }
        return new ApiException(500, mensajeGenerico);
    }
}
