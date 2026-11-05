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
 * Arma las rutas que toda tabla necesita (listar, ver una, crear, modificar y borrar) para no
 * repetirlas módulo por módulo. Lo distinto de cada módulo se le pasa por parámetro: cómo filtrar la
 * lista, quién puede escribir, y qué revisar antes de guardar. Lo hace sobre cualquier tabla porque el
 * tipo de objeto y el DAO van escritos como {@code <T>}; quien lo crea dice cuál, por ejemplo
 * {@code new CrudController<>(cursos, Curso.class, "cursos", "el curso")} en {@link ModulosCrud}.
 *
 * El rol se exige siempre aquí, dentro del servidor, mirando la sesión. Ocultar un botón en la página
 * no protege nada: la ruta HTTP se puede llamar a mano, así que cada petición vuelve a comprobar quién
 * la manda. Los controles propios de cada módulo (cupo disponible, que la sección sea del profesor que
 * escribe, datos repetidos) entran por {@code antesEscribir} y {@code antesEliminar}.
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

    /** Los cuatro datos fijos: el DAO de la tabla, el tipo de objeto, el trozo de URL y cómo se nombra en los mensajes. */
    public CrudController(CRUD<T> dao, Class<T> tipo, String ruta, String nombreEntidad) {
        this.dao = dao;
        this.tipo = tipo;
        this.ruta = ruta;
        this.nombreEntidad = nombreEntidad;
        // Si el módulo no pide un filtro raro, la lista completa es la respuesta correcta.
        this.listado = ctx -> dao.listarTodos();
    }

    // Cada uno de los métodos siguientes guarda un trozo distinto del comportamiento y devuelve este
    // mismo controlador, así que {@link ModulosCrud} los puede escribir seguidos en una sola línea.

    /** Cómo se arma la lista que devuelve el GET, normalmente filtrando por los números de la URL. */
    public CrudController<T> listado(Function<Context, List<T>> listado) {
        this.listado = listado;
        return this;
    }

    /** Qué campo del objeto recibe el id que viene en la dirección. */
    public CrudController<T> fijarId(BiConsumer<T, Integer> fijarId) {
        this.fijarId = fijarId;
        return this;
    }

    /** Comprobación de campos antes de guardar. */
    public CrudController<T> valido(Predicate<T> valido) {
        this.valido = valido;
        return this;
    }

    /** Roles que pueden leer. Por defecto administrativo y profesor. */
    public CrudController<T> rolesLectura(String... roles) {
        this.rolesLectura = List.of(roles);
        return this;
    }

    /** Roles que pueden crear, modificar y borrar. Por defecto solo administrativo. */
    public CrudController<T> rolesEscritura(String... roles) {
        this.rolesEscritura = List.of(roles);
        return this;
    }

    /** Lo que hay que revisar o corregir antes de insertar o actualizar. */
    public CrudController<T> antesEscribir(BiConsumer<Context, T> antesEscribir) {
        this.antesEscribir = antesEscribir;
        return this;
    }

    /** Lo que impide borrar: una sección con matriculados, por ejemplo, no se elimina. */
    public CrudController<T> antesEliminar(BiConsumer<Context, Integer> antesEliminar) {
        this.antesEliminar = antesEliminar;
        return this;
    }

    /** Un último paso sobre la entidad ya guardada, antes de responder a la página. */
    public CrudController<T> despuesEscribir(Function<T, T> despuesEscribir) {
        this.despuesEscribir = despuesEscribir;
        return this;
    }

    /** Texto que se muestra cuando la base de datos rechaza el dato por repetido. */
    public CrudController<T> mensajeConflicto(String mensajeConflicto) {
        this.mensajeConflicto = mensajeConflicto;
        return this;
    }

    /** Monta las cinco rutas de esta tabla en la aplicación. */
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
            // Primero se comprueba que la fila existe; sin esto, actualizar no falla y contestaría 200
            // como si hubiera modificado algo.
            if (dao.leerPorId(id) == null) {
                throw new ApiException(404, "No se encontró " + nombreEntidad + ".");
            }
            T entidad = Http.leerJson(ctx, tipo);
            validar(entidad);
            fijarId.accept(entidad, id); // manda el id de la dirección, no el que alguien escriba en el JSON
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

    /** Campos vacíos o fuera de rango: se responde 400 con la entidad nombrada en el mensaje. */
    private void validar(T entidad) {
        if (!valido.test(entidad)) {
            throw new ApiException(400, "Revise los campos de " + nombreEntidad + ": hay datos vacíos o fuera de rango.");
        }
    }

    /**
     * Traduce el error de la base de datos al código HTTP que le toca. MySQL avisa con un número de
     * error propio por qué regla se incumplió: si falta una fila referenciada el problema es del dato
     * enviado (400), si el borrado está bloqueado o el dato se repite es un conflicto con lo ya
     * guardado (409), y si nada de eso cuadra es algo nuestro (500). El detalle técnico del controlador
     * JDBC no se manda al navegador.
     */
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
