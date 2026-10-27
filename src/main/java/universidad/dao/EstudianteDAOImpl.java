package universidad.dao;

import universidad.conexion.Conexion;
import universidad.modelo.Estudiante;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EstudianteDAOImpl implements EstudianteDAO {

    @Override
    public List<Estudiante> listarTodos() {
        List<Estudiante> lista = new ArrayList<>();
        String sql = "SELECT * FROM estudiantes";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearEstudiante(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar los estudiantes.", ex);
        }
        return lista;
    }

    @Override
    public Estudiante leerPorId(int id) {
        String sql = "SELECT * FROM estudiantes WHERE id_estudiante = ?";
        Estudiante est = null;

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    est = mapearEstudiante(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar el estudiante.", ex);
        }
        return est;
    }

    @Override
    public int registrar(Estudiante e) throws SQLException {
        String sql = "INSERT INTO estudiantes (nombre, apellido, email, fecha_nacimiento, id_carrera) VALUES (?, ?, ?, ?, ?)";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getApellido());
            ps.setString(3, e.getEmail());
            ps.setDate(4, e.getFechaNacimiento());
            ps.setInt(5, e.getIdCarrera());
            int filasAfectadas = ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setIdEstudiante(keys.getInt(1));
                }
            }

            return filasAfectadas;
        }
    }

    @Override
    public int actualizar(Estudiante e) throws SQLException {
        String sql = "UPDATE estudiantes SET nombre=?, apellido=?, email=?, fecha_nacimiento=?, id_carrera=? WHERE id_estudiante=?";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getApellido());
            ps.setString(3, e.getEmail());
            ps.setDate(4, e.getFechaNacimiento());
            ps.setInt(5, e.getIdCarrera());
            ps.setInt(6, e.getIdEstudiante());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM estudiantes WHERE id_estudiante = ?";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private Estudiante mapearEstudiante(ResultSet rs) throws SQLException {
        Estudiante e = new Estudiante();
        e.setIdEstudiante(rs.getInt("id_estudiante"));
        e.setNombre(rs.getString("nombre"));
        e.setApellido(rs.getString("apellido"));
        e.setEmail(rs.getString("email"));
        e.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
        e.setIdCarrera(rs.getInt("id_carrera"));
        return e;
    }
}