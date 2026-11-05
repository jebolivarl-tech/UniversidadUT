package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Carrera;

/**
 * DAO de las carreras: la tabla carreras solo guarda nombre y a qué facultad pertenece, con un número que
 * pone MySQL. Todo lo que se mueve aquí son órdenes SQL enviadas por JDBC, y cada conexión sale del pool de
 * conexiones para no abrir una nueva por consulta.
 * Como la columna del nombre no admite repetidos, dos carreras con el mismo nombre hacen que MySQL
 * rechace el guardado y el error suba hasta el formulario.
 */
public class CarreraDAOImpl implements CarreraDAO {

    // No hay nada que preparar antes de consultar: la clase no guarda estado, solo manda órdenes.
    public CarreraDAOImpl() {
    }

    // Tabla completa y sin filtro. Casi siempre la llaman para traducir números por nombre al armar
    // desplegables y respuestas.
    @Override
    public List<Carrera> listarTodos() {
        List<Carrera> lista = new ArrayList<>();
        String sql = "SELECT * FROM carreras";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCarrera(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar las carreras.", ex);
        }
        return lista;
    }

    // Si el id no está, la variable se queda como llegó: sin carrera. El controlador decide el 404.
    @Override
    public Carrera leerPorId(int id) {
        String sql = "SELECT * FROM carreras WHERE id_carrera = ?";
        Carrera carrera = null;
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    carrera = mapearCarrera(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la carrera.", ex);
        }
        return carrera;
    }

    // El número de la carrera lo escoge MySQL, no la aplicación. Al pedir los datos recién guardados, el
    // objeto se queda con ese número y el formulario ya puede editar la fila que acaba de crear.
    @Override
    public int registrar(Carrera c) throws SQLException {
        String sql = "INSERT INTO carreras (nombre, id_facultad) VALUES (?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getIdFacultad());
            int filasAfectadas = ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdCarrera(keys.getInt(1));
                }
            }
            return filasAfectadas;
        }
    }

    // El WHERE del final no cambia ningún dato: solo le dice a MySQL cuál carrera es la que tiene que
    // tocar con los valores de arriba.
    @Override
    public int actualizar(Carrera c) throws SQLException {
        String sql = "UPDATE carreras SET nombre = ?, id_facultad = ? WHERE id_carrera = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getIdFacultad());
            ps.setInt(3, c.getIdCarrera());
            return ps.executeUpdate();
        }
    }

    // Borra una sola fila y dice cuántas fueron. Si la carrera todavía tiene estudiantes, MySQL lo
    // impide para no dejar alumnos colgando; ese error sube y la pantalla lo muestra.
    @Override
    public int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM carreras WHERE id_carrera = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Convierte la fila que contestó MySQL en objeto Carrera, columna por columna.
    private Carrera mapearCarrera(ResultSet rs) throws SQLException {
        Carrera c = new Carrera();
        c.setIdCarrera(rs.getInt("id_carrera"));
        c.setNombre(rs.getString("nombre"));
        c.setIdFacultad(rs.getInt("id_facultad"));
        return c;
    }
}
