package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Facultad;

/**
 * DAO de las facultades, la punta del árbol: facultad contiene carreras, carreras contienen cursos, cursos
 * se dictan en secciones y en esas secciones caen las matrículas. Esta clase solo mueve la primera capa, y
 * lo hace con órdenes SQL sobre MySQL a través de JDBC.
 * El decano se guarda como texto escrito a mano, no como una ficha enlazada: es un dato de contacto, no
 * alguien que tenga usuario en el sistema.
 */
public class FacultadDAOImpl implements FacultadDAO {

    // Ordenadas por nombre porque la lista se muestra tal cual en pantalla.
    @Override
    public List<Facultad> listarTodos() {
        List<Facultad> lista = new ArrayList<>();
        String sql = "SELECT * FROM facultades ORDER BY nombre";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar las facultades.", ex);
        }
        return lista;
    }

    // Null cuando el id no existe en la tabla.
    @Override
    public Facultad leerPorId(int id) {
        String sql = "SELECT * FROM facultades WHERE id_facultad = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la facultad.", ex);
        }
    }

    // MySQL numera la fila nueva; ese número se recoge justo después de guardar y se copia al objeto para
    // que el formulario sepa qué facultad acaba de crear.
    @Override
    public int registrar(Facultad f) throws SQLException {
        String sql = "INSERT INTO facultades (nombre, decano) VALUES (?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNombre());
            ps.setString(2, f.getDecano());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    f.setIdFacultad(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    // El nombre no admite repetidos; si alguien registra dos veces la misma facultad, MySQL contesta con
    // error y ese error es el que ve el usuario, no un borrado silencioso.
    @Override
    public int actualizar(Facultad f) throws SQLException {
        String sql = "UPDATE facultades SET nombre = ?, decano = ? WHERE id_facultad = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, f.getNombre());
            ps.setString(2, f.getDecano());
            ps.setInt(3, f.getIdFacultad());
            return ps.executeUpdate();
        }
    }

    // Se lleva por delante los profesores de la facultad, pero las carreras atan: si todavía hay alguna,
    // MySQL rechaza el borrado para no dejarlas huérfanas.
    @Override
    public int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM facultades WHERE id_facultad = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Convierte la fila contestada por MySQL en objeto Facultad, buscando cada columna por su nombre.
    private Facultad mapear(ResultSet rs) throws SQLException {
        Facultad f = new Facultad();
        f.setIdFacultad(rs.getInt("id_facultad"));
        f.setNombre(rs.getString("nombre"));
        f.setDecano(rs.getString("decano"));
        return f;
    }
}
