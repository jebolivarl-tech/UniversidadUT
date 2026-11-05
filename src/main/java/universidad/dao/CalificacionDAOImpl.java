package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Calificacion;

/**
 * DAO de las calificaciones: lee y escribe la tabla calificaciones de MySQL con JDBC, la librería de Java
 * que habla con la base de datos. Las conexiones se piden al pool de conexiones ( Conexion.getConexion )
 * y se devuelven solas al terminar cada bloque.
 * El promedio nunca se escribe: es una columna que MySQL arma él solo con las tres notas (30 + 30 + 40).
 * Por eso no está en el INSERT ni en el UPDATE, y sí al leer.
 */
public class CalificacionDAOImpl implements CalificacionDAO {

    // Las dos órdenes de escritura viven aquí para no reescribirlas. Fíjese en la lista de columnas:
    // el promedio no aparece, y esa es la intención.
    private static final String INSERT = """
            INSERT INTO calificaciones (id_estudiante, id_seccion, nota_parcial1, nota_parcial2, nota_examen_final, estado)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE calificaciones SET id_estudiante = ?, id_seccion = ?, nota_parcial1 = ?, nota_parcial2 = ?,
            nota_examen_final = ?, estado = ? WHERE id_calificacion = ?
            """;

    @Override
    public List<Calificacion> listarTodos() {
        // El null le dice al método de abajo que esta consulta no lleva valores.
        return consultar("SELECT * FROM calificaciones ORDER BY id_seccion, id_estudiante", null);
    }

    // La hoja de notas de un grupo, renglón por renglón.
    @Override
    public List<Calificacion> listarPorSeccion(int idSeccion) {
        return consultar("SELECT * FROM calificaciones WHERE id_seccion = ? ORDER BY id_estudiante", idSeccion);
    }

    // Boletín de un estudiante: todos sus grupos, sin importar el periodo.
    @Override
    public List<Calificacion> listarPorEstudiante(int idEstudiante) {
        return consultar("SELECT * FROM calificaciones WHERE id_estudiante = ? ORDER BY id_seccion", idEstudiante);
    }

    // La base no deja dos fichas del mismo estudiante en el mismo grupo, así que esta consulta trae
    // una fila o ninguna.
    @Override
    public Calificacion leerPorEstudianteYSeccion(int idEstudiante, int idSeccion) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(
                     "SELECT * FROM calificaciones WHERE id_estudiante = ? AND id_seccion = ?")) {
            ps.setInt(1, idEstudiante);
            ps.setInt(2, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la calificación.", ex);
        }
    }

    @Override
    public Calificacion leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM calificaciones WHERE id_calificacion = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la calificación.", ex);
        }
    }

    // Al guardar, MySQL escoge el número de la fila; aquí se pide ese número recién dado y se copia al
    // objeto, para que la pantalla pueda seguir trabajando con la misma calificación.
    @Override
    public int registrar(Calificacion c) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            int posicion = escribirCuerpo(ps, c);
            ps.setString(posicion, c.getEstado());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdCalificacion(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Calificacion c) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(UPDATE)) {
            int posicion = escribirCuerpo(ps, c);
            ps.setString(posicion, c.getEstado());
            // El id va en el último hueco: no se cambia, solo dice a qué fila tocar.
            ps.setInt(posicion + 1, c.getIdCalificacion());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM calificaciones WHERE id_calificacion = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Coloca las dos ids y las tres notas en los cinco primeros huecos, y devuelve el número del hueco
    // libre siguiente. El estado se escribe aparte, en las dos sentencias.
    private static int escribirCuerpo(PreparedStatement ps, Calificacion c) throws SQLException {
        ps.setInt(1, c.getIdEstudiante());
        ps.setInt(2, c.getIdSeccion());
        ponerNota(ps, 3, c.getNotaParcial1());
        ponerNota(ps, 4, c.getNotaParcial2());
        ponerNota(ps, 5, c.getNotaExamenFinal());
        return 6;
    }

    // Si el profesor todavía no digitó la nota, se manda un vacío (NULL), nunca un cero: en la escala de
    // 0 a 5 un cero es una nota presentada, y un trabajo sin calificar no lo es.
    private static void ponerNota(PreparedStatement ps, int posicion, Double nota) throws SQLException {
        if (nota == null) {
            ps.setNull(posicion, Types.DECIMAL);
        } else {
            ps.setDouble(posicion, nota);
        }
    }

    // Los listar comparten este método: si le pasan un valor, va en el primer signo de interrogación;
    // después arma un objeto por cada fila de la respuesta.
    private List<Calificacion> consultar(String sql, Integer parametro) {
        List<Calificacion> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar las calificaciones.", ex);
        }
        return lista;
    }

    // Arma el objeto con las columnas de la fila leída. promedio_final es la que MySQL calcula; Java
    // únicamente la copia.
    private Calificacion mapear(ResultSet rs) throws SQLException {
        Calificacion c = new Calificacion();
        c.setIdCalificacion(rs.getInt("id_calificacion"));
        c.setIdEstudiante(rs.getInt("id_estudiante"));
        c.setIdSeccion(rs.getInt("id_seccion"));
        c.setNotaParcial1(leerNota(rs, "nota_parcial1"));
        c.setNotaParcial2(leerNota(rs, "nota_parcial2"));
        c.setNotaExamenFinal(leerNota(rs, "nota_examen_final"));
        c.setPromedioFinal(leerNota(rs, "promedio_final"));
        c.setEstado(rs.getString("estado"));
        return c;
    }

    // Pedir una columna vacía como número devuelve 0.0 y se perdería el dato, así que después de leer
    // se pregunta si lo que llegó era en realidad un vacío.
    private static Double leerNota(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }
}
