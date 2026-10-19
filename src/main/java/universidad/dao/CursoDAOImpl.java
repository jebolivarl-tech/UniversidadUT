package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Curso;

public class CursoDAOImpl implements CursoDAO {

    @Override
    public List<Curso> listarTodos() {
        return consultar("SELECT * FROM cursos ORDER BY nombre", null);
    }

    @Override
    public List<Curso> listarPorCarrera(int idCarrera) {
        return consultar("SELECT * FROM cursos WHERE id_carrera = ? ORDER BY nombre", idCarrera);
    }

    @Override
    public List<Curso> listarPorProfesor(int idProfesor) {
        return consultar("SELECT * FROM cursos WHERE id_profesor = ? ORDER BY nombre", idProfesor);
    }

    @Override
    public Curso leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM cursos WHERE id_curso = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar el curso.", ex);
        }
    }

    @Override
    public int registrar(Curso c) throws SQLException {
        String sql = "INSERT INTO cursos (nombre, creditos, id_carrera, id_profesor) VALUES (?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getCreditos());
            ps.setInt(3, c.getIdCarrera());
            ps.setInt(4, c.getIdProfesor());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdCurso(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Curso c) throws SQLException {
        String sql = "UPDATE cursos SET nombre = ?, creditos = ?, id_carrera = ?, id_profesor = ? WHERE id_curso = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setInt(2, c.getCreditos());
            ps.setInt(3, c.getIdCarrera());
            ps.setInt(4, c.getIdProfesor());
            ps.setInt(5, c.getIdCurso());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM cursos WHERE id_curso = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private List<Curso> consultar(String sql, Integer parametro) {
        List<Curso> lista = new ArrayList<>();
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
            throw new IllegalStateException("No fue posible listar los cursos.", ex);
        }
        return lista;
    }

    private Curso mapear(ResultSet rs) throws SQLException {
        Curso c = new Curso();
        c.setIdCurso(rs.getInt("id_curso"));
        c.setNombre(rs.getString("nombre"));
        c.setCreditos(rs.getInt("creditos"));
        c.setIdCarrera(rs.getInt("id_carrera"));
        c.setIdProfesor(rs.getInt("id_profesor"));
        return c;
    }
}
