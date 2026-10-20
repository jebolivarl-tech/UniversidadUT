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

    /** Cada sección se devuelve con el nombre del curso, el del periodo y sus cupos ya ocupados. */
    private static final String BASE = """
            SELECT s.id_seccion, s.id_curso, s.id_profesor, s.id_periodo, s.aula, s.cupo_maximo,
                   c.nombre AS nombre_curso, p.nombre AS nombre_periodo,
                   (SELECT COUNT(*) FROM matriculas m WHERE m.id_seccion = s.id_seccion) AS cupos_ocupados
            FROM secciones s
            JOIN cursos c ON c.id_curso = s.id_curso
            JOIN periodos_academicos p ON p.id_periodo = s.id_periodo
            """;

    private static final String POR_ESTUDIANTE = """
            SELECT DISTINCT s.id_seccion, s.id_curso, s.id_profesor, s.id_periodo, s.aula, s.cupo_maximo,
                   c.nombre AS nombre_curso, p.nombre AS nombre_periodo,
                   (SELECT COUNT(*) FROM matriculas m2 WHERE m2.id_seccion = s.id_seccion) AS cupos_ocupados
            FROM secciones s
            JOIN cursos c ON c.id_curso = s.id_curso
            JOIN periodos_academicos p ON p.id_periodo = s.id_periodo
            JOIN matriculas m ON m.id_seccion = s.id_seccion
            WHERE m.id_estudiante = ?
            ORDER BY s.id_periodo DESC, s.id_seccion
            """;

    @Override
    public List<Seccion> listarTodos() {
        return consultar(BASE + " ORDER BY s.id_periodo DESC, s.id_seccion", null);
    }

    @Override
    public List<Seccion> listarPorProfesor(int idProfesor) {
        return consultar(BASE + " WHERE s.id_profesor = ? ORDER BY s.id_periodo DESC, s.id_seccion", idProfesor);
    }

    @Override
    public List<Seccion> listarPorPeriodo(int idPeriodo) {
        return consultar(BASE + " WHERE s.id_periodo = ? ORDER BY s.id_seccion", idPeriodo);
    }

    @Override
    public List<Seccion> listarPorEstudiante(int idEstudiante) {
        return consultar(POR_ESTUDIANTE, idEstudiante);
    }

    /**
     * Grupos que el estudiante podría inscribir: de su carrera, con el periodo abierto, sin tener ya
     * una matrícula en ese curso y periodo (`uq_matricula`) y con cupo disponible. El tope de créditos
     * lo filtra el servicio, que es donde están las reglas.
     */
    @Override
    public List<Seccion> listarInscribiblesPara(int idEstudiante) {
        String sql = BASE + """
                 JOIN estudiantes e ON e.id_carrera = c.id_carrera
                WHERE e.id_estudiante = ? AND p.estado = 'ACTIVO'
                  AND NOT EXISTS (SELECT 1 FROM matriculas ya
                                  WHERE ya.id_estudiante = e.id_estudiante
                                    AND ya.id_curso = s.id_curso
                                    AND ya.periodo = p.nombre)
                  AND (SELECT COUNT(*) FROM matriculas ocupada
                       WHERE ocupada.id_seccion = s.id_seccion) < s.cupo_maximo
                ORDER BY c.nombre, s.id_seccion
                """;
        return consultar(sql, idEstudiante);
    }

    @Override
    public Seccion leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(BASE + " WHERE s.id_seccion = ?")) {
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
        s.setNombreCurso(rs.getString("nombre_curso"));
        s.setNombrePeriodo(rs.getString("nombre_periodo"));
        s.setCuposOcupados(rs.getInt("cupos_ocupados"));
        return s;
    }
}
