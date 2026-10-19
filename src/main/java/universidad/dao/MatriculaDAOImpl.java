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
import universidad.modelo.Matricula;

public class MatriculaDAOImpl implements MatriculaDAO {

    @Override
    public List<Matricula> listarTodos() {
        return consultar("SELECT * FROM matriculas ORDER BY periodo DESC, id_matricula", null);
    }

    @Override
    public List<Matricula> listarPorEstudiante(int idEstudiante) {
        return consultar("SELECT * FROM matriculas WHERE id_estudiante = ? ORDER BY periodo DESC, id_matricula", idEstudiante);
    }

    @Override
    public List<Matricula> listarPorCurso(int idCurso) {
        return consultar("SELECT * FROM matriculas WHERE id_curso = ? ORDER BY periodo DESC, id_matricula", idCurso);
    }

    @Override
    public Matricula leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM matriculas WHERE id_matricula = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la matrícula.", ex);
        }
    }

    @Override
    public int registrar(Matricula m) throws SQLException {
        String sql = "INSERT INTO matriculas (id_estudiante, id_curso, periodo, nota) VALUES (?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getIdEstudiante());
            ps.setInt(2, m.getIdCurso());
            ps.setString(3, m.getPeriodo());
            ponerNota(ps, 4, m.getNota());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setIdMatricula(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Matricula m) throws SQLException {
        String sql = "UPDATE matriculas SET id_estudiante = ?, id_curso = ?, periodo = ?, nota = ? WHERE id_matricula = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, m.getIdEstudiante());
            ps.setInt(2, m.getIdCurso());
            ps.setString(3, m.getPeriodo());
            ponerNota(ps, 4, m.getNota());
            ps.setInt(5, m.getIdMatricula());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM matriculas WHERE id_matricula = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private static void ponerNota(PreparedStatement ps, int posicion, Double nota) throws SQLException {
        if (nota == null) {
            ps.setNull(posicion, Types.DECIMAL);
        } else {
            ps.setDouble(posicion, nota);
        }
    }

    private List<Matricula> consultar(String sql, Integer parametro) {
        List<Matricula> lista = new ArrayList<>();
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
            throw new IllegalStateException("No fue posible listar las matrículas.", ex);
        }
        return lista;
    }

    private Matricula mapear(ResultSet rs) throws SQLException {
        Matricula m = new Matricula();
        m.setIdMatricula(rs.getInt("id_matricula"));
        m.setIdEstudiante(rs.getInt("id_estudiante"));
        m.setIdCurso(rs.getInt("id_curso"));
        m.setPeriodo(rs.getString("periodo"));
        double nota = rs.getDouble("nota");
        m.setNota(rs.wasNull() ? null : nota);
        return m;
    }
}
