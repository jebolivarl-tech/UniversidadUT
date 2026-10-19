package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Seccion;

public class SeccionDAOImpl implements SeccionDAO {

    private static final String POR_ESTUDIANTE = """
            SELECT DISTINCT s.* FROM secciones s
            JOIN cursos c ON c.id_curso = s.id_curso
            JOIN matriculas m ON m.id_curso = c.id_curso
            WHERE m.id_estudiante = ?
            ORDER BY s.id_periodo DESC, s.id_seccion
            """;

    @Override
    public List<Seccion> listarTodos() {
        return consultar("SELECT * FROM secciones ORDER BY id_periodo DESC, id_seccion", null);
    }

    @Override
    public List<Seccion> listarPorProfesor(int idProfesor) {
        return consultar("SELECT * FROM secciones WHERE id_profesor = ? ORDER BY id_periodo DESC, id_seccion", idProfesor);
    }

    @Override
    public List<Seccion> listarPorPeriodo(int idPeriodo) {
        return consultar("SELECT * FROM secciones WHERE id_periodo = ? ORDER BY id_seccion", idPeriodo);
    }

    @Override
    public List<Seccion> listarPorEstudiante(int idEstudiante) {
        return consultar(POR_ESTUDIANTE, idEstudiante);
    }

    @Override
    public Seccion leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM secciones WHERE id_seccion = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la sección.", ex);
        }
    }

    @Override
    public int registrar(Seccion s) throws SQLException {
        String sql = "INSERT INTO secciones (id_curso, id_profesor, id_periodo, aula, cupo_maximo) VALUES (?, ?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getIdCurso());
            ps.setInt(2, s.getIdProfesor());
            ps.setInt(3, s.getIdPeriodo());
            ps.setString(4, s.getAula());
            ps.setInt(5, s.getCupoMaximo());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setIdSeccion(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Seccion s) throws SQLException {
        String sql = "UPDATE secciones SET id_curso = ?, id_profesor = ?, id_periodo = ?, aula = ?, cupo_maximo = ? WHERE id_seccion = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, s.getIdCurso());
            ps.setInt(2, s.getIdProfesor());
            ps.setInt(3, s.getIdPeriodo());
            ps.setString(4, s.getAula());
            ps.setInt(5, s.getCupoMaximo());
            ps.setInt(6, s.getIdSeccion());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM secciones WHERE id_seccion = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private List<Seccion> consultar(String sql, Integer parametro) {
        List<Seccion> lista = new ArrayList<>();
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
            throw new IllegalStateException("No fue posible listar las secciones.", ex);
        }
        return lista;
    }

    private Seccion mapear(ResultSet rs) throws SQLException {
        Seccion s = new Seccion();
        s.setIdSeccion(rs.getInt("id_seccion"));
        s.setIdCurso(rs.getInt("id_curso"));
        s.setIdProfesor(rs.getInt("id_profesor"));
        s.setIdPeriodo(rs.getInt("id_periodo"));
        s.setAula(rs.getString("aula"));
        s.setCupoMaximo(rs.getInt("cupo_maximo"));
        return s;
    }
}
