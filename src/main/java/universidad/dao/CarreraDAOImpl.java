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

public class CarreraDAOImpl implements CarreraDAO {

    public CarreraDAOImpl() {
    }

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

    @Override
    public int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM carreras WHERE id_carrera = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private Carrera mapearCarrera(ResultSet rs) throws SQLException {
        Carrera c = new Carrera();
        c.setIdCarrera(rs.getInt("id_carrera"));
        c.setNombre(rs.getString("nombre"));
        c.setIdFacultad(rs.getInt("id_facultad"));
        return c;
    }
}
