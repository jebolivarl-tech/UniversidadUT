package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Profesor;

public class ProfesorDAOImpl implements ProfesorDAO {

    @Override
    public List<Profesor> listarTodos() {
        return consultar("SELECT * FROM profesores ORDER BY apellido, nombre", null);
    }

    @Override
    public List<Profesor> listarPorFacultad(int idFacultad) {
        return consultar("SELECT * FROM profesores WHERE id_facultad = ? ORDER BY apellido, nombre", idFacultad);
    }

    @Override
    public Profesor leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM profesores WHERE id_profesor = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar el profesor.", ex);
        }
    }

    @Override
    public int registrar(Profesor p) throws SQLException {
        String sql = "INSERT INTO profesores (nombre, apellido, email, id_facultad) VALUES (?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getApellido());
            ps.setString(3, p.getEmail());
            ps.setInt(4, p.getIdFacultad());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setIdProfesor(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Profesor p) throws SQLException {
        String sql = "UPDATE profesores SET nombre = ?, apellido = ?, email = ?, id_facultad = ? WHERE id_profesor = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getApellido());
            ps.setString(3, p.getEmail());
            ps.setInt(4, p.getIdFacultad());
            ps.setInt(5, p.getIdProfesor());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM profesores WHERE id_profesor = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private List<Profesor> consultar(String sql, Integer parametro) {
        List<Profesor> lista = new ArrayList<>();
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
            throw new IllegalStateException("No fue posible listar los profesores.", ex);
        }
        return lista;
    }

    private Profesor mapear(ResultSet rs) throws SQLException {
        Profesor p = new Profesor();
        p.setIdProfesor(rs.getInt("id_profesor"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        p.setEmail(rs.getString("email"));
        p.setIdFacultad(rs.getInt("id_facultad"));
        return p;
    }
}
