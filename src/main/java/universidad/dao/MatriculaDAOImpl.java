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

    private static final String CONSULTA = """
            SELECT m.id_matricula, m.id_estudiante, m.id_seccion, m.id_curso, m.periodo, m.nota, m.estado
            FROM matriculas m
            """;

    @Override
    public List<Matricula> listarTodos() {
        return consultar(CONSULTA + " ORDER BY m.periodo DESC, m.id_matricula", new Object[0]);
    }

    @Override
    public List<Matricula> listarPorEstudiante(int idEstudiante) {
        return consultar(CONSULTA + " WHERE m.id_estudiante = ? ORDER BY m.periodo DESC, m.id_matricula", idEstudiante);
    }

    @Override
    public List<Matricula> listarPorCurso(int idCurso) {
        return consultar(CONSULTA + " WHERE m.id_curso = ? ORDER BY m.periodo DESC, m.id_matricula", idCurso);
    }

    @Override
    public List<Matricula> listarPorSeccion(int idSeccion) {
        return consultar(CONSULTA + " WHERE m.id_seccion = ? ORDER BY m.id_matricula", idSeccion);
    }

    @Override
    public List<Matricula> listarPorEstudianteYPeriodo(int idEstudiante, String periodo) {
        return consultar(CONSULTA + " WHERE m.id_estudiante = ? AND m.periodo = ? ORDER BY m.id_matricula",
                idEstudiante, periodo);
    }

    @Override
    public int contarPorSeccion(int idSeccion) {
        String sql = "SELECT COUNT(*) FROM matriculas WHERE id_seccion = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible contar los matriculados de la sección.", ex);
        }
    }

    @Override
    public Matricula leerPorEstudianteYSeccion(int idEstudiante, int idSeccion) {
        List<Matricula> encontradas = consultar(
                CONSULTA + " WHERE m.id_estudiante = ? AND m.id_seccion = ?", idEstudiante, idSeccion);
        return encontradas.isEmpty() ? null : encontradas.get(0);
    }

    @Override
    public Matricula leerPorId(int id) {
        List<Matricula> encontradas = consultar(CONSULTA + " WHERE m.id_matricula = ?", id);
        return encontradas.isEmpty() ? null : encontradas.get(0);
    }

    @Override
    public int registrar(Matricula m) throws SQLException {
        String sql = "INSERT INTO matriculas (id_estudiante, id_seccion, id_curso, periodo, nota, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getIdEstudiante());
            ps.setInt(2, m.getIdSeccion());
            ps.setInt(3, m.getIdCurso());
            ps.setString(4, m.getPeriodo());
            ponerNota(ps, 5, m.getNota());
            ps.setString(6, m.getEstado());
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
        String sql = "UPDATE matriculas SET id_estudiante = ?, id_seccion = ?, id_curso = ?, periodo = ?, "
                + "nota = ?, estado = ? WHERE id_matricula = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, m.getIdEstudiante());
            ps.setInt(2, m.getIdSeccion());
            ps.setInt(3, m.getIdCurso());
            ps.setString(4, m.getPeriodo());
            ponerNota(ps, 5, m.getNota());
            ps.setString(6, m.getEstado());
            ps.setInt(7, m.getIdMatricula());
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

    private List<Matricula> consultar(String sql, Object... parametros) {
        List<Matricula> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                Object p = parametros[i];
                if (p instanceof String texto) {
                    ps.setString(i + 1, texto);
                } else {
                    ps.setInt(i + 1, (Integer) p);
                }
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
        m.setIdSeccion(rs.getInt("id_seccion"));
        m.setIdCurso(rs.getInt("id_curso"));
        m.setPeriodo(rs.getString("periodo"));
        double nota = rs.getDouble("nota");
        m.setNota(rs.wasNull() ? null : nota);
        m.setEstado(rs.getString("estado"));
        return m;
    }
}
