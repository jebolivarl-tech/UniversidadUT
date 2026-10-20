package universidad.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.Asistencia;

public class AsistenciaDAOImpl implements AsistenciaDAO {

    @Override
    public List<Asistencia> listarTodos() {
        return consultar("SELECT * FROM asistencias ORDER BY fecha DESC, id_asistencia", new Object[0]);
    }

    @Override
    public List<Asistencia> listarPorSeccion(int idSeccion) {
        return consultar("SELECT * FROM asistencias WHERE id_seccion = ? ORDER BY fecha DESC, id_asistencia", idSeccion);
    }

    @Override
    public List<Asistencia> listarPorSeccionYFecha(int idSeccion, Date fecha) {
        return consultar("SELECT * FROM asistencias WHERE id_seccion = ? AND fecha = ? ORDER BY id_asistencia", idSeccion, fecha);
    }

    @Override
    public List<Asistencia> listarPorEstudiante(int idEstudiante) {
        return consultar("SELECT * FROM asistencias WHERE id_estudiante = ? ORDER BY fecha DESC, id_asistencia", idEstudiante);
    }

    @Override
    public List<Asistencia> listarPorEstudianteYSeccion(int idEstudiante, int idSeccion) {
        return consultar("SELECT * FROM asistencias WHERE id_estudiante = ? AND id_seccion = ? ORDER BY fecha",
                idEstudiante, idSeccion);
    }

    @Override
    public int contarPorSeccionYEstado(int idSeccion, String estado) {
        String sql = "SELECT COUNT(*) FROM asistencias WHERE id_seccion = ? AND estado = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            ps.setString(2, estado);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible contar las asistencias.", ex);
        }
    }

    @Override
    public Asistencia leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM asistencias WHERE id_asistencia = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la asistencia.", ex);
        }
    }

    @Override
    public int registrar(Asistencia a) throws SQLException {
        String sql = "INSERT INTO asistencias (id_estudiante, id_seccion, fecha, estado) VALUES (?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getIdEstudiante());
            ps.setInt(2, a.getIdSeccion());
            ps.setDate(3, a.getFecha());
            ps.setString(4, a.getEstado());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setIdAsistencia(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Asistencia a) throws SQLException {
        String sql = "UPDATE asistencias SET id_estudiante = ?, id_seccion = ?, fecha = ?, estado = ? WHERE id_asistencia = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, a.getIdEstudiante());
            ps.setInt(2, a.getIdSeccion());
            ps.setDate(3, a.getFecha());
            ps.setString(4, a.getEstado());
            ps.setInt(5, a.getIdAsistencia());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM asistencias WHERE id_asistencia = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private List<Asistencia> consultar(String sql, Object... parametros) {
        List<Asistencia> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                Object parametro = parametros[i];
                if (parametro instanceof Date fecha) {
                    ps.setDate(i + 1, fecha);
                } else {
                    ps.setInt(i + 1, (Integer) parametro);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar las asistencias.", ex);
        }
        return lista;
    }

    private Asistencia mapear(ResultSet rs) throws SQLException {
        Asistencia a = new Asistencia();
        a.setIdAsistencia(rs.getInt("id_asistencia"));
        a.setIdEstudiante(rs.getInt("id_estudiante"));
        a.setIdSeccion(rs.getInt("id_seccion"));
        a.setFecha(rs.getDate("fecha"));
        a.setEstado(rs.getString("estado"));
        return a;
    }
}
