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

public class FacultadDAOImpl implements FacultadDAO {

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

    @Override
    public int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM facultades WHERE id_facultad = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private Facultad mapear(ResultSet rs) throws SQLException {
        Facultad f = new Facultad();
        f.setIdFacultad(rs.getInt("id_facultad"));
        f.setNombre(rs.getString("nombre"));
        f.setDecano(rs.getString("decano"));
        return f;
    }
}
