package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import universidad.conexion.Conexion;
import universidad.modelo.PeriodoAcademico;

public class PeriodoAcademicoDAOImpl implements PeriodoAcademicoDAO {

    @Override
    public List<PeriodoAcademico> listarTodos() {
        return consultar("SELECT * FROM periodos_academicos ORDER BY fecha_inicio DESC", null);
    }

    @Override
    public List<PeriodoAcademico> listarPorEstado(String estado) {
        return consultar("SELECT * FROM periodos_academicos WHERE estado = ? ORDER BY fecha_inicio DESC", estado);
    }

    @Override
    public PeriodoAcademico leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM periodos_academicos WHERE id_periodo = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar el periodo.", ex);
        }
    }

    @Override
    public int registrar(PeriodoAcademico p) throws SQLException {
        String sql = "INSERT INTO periodos_academicos (nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setDate(2, p.getFechaInicio());
            ps.setDate(3, p.getFechaFin());
            ps.setString(4, p.getEstado());
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setIdPeriodo(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(PeriodoAcademico p) throws SQLException {
        String sql = "UPDATE periodos_academicos SET nombre = ?, fecha_inicio = ?, fecha_fin = ?, estado = ? WHERE id_periodo = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setDate(2, p.getFechaInicio());
            ps.setDate(3, p.getFechaFin());
            ps.setString(4, p.getEstado());
            ps.setInt(5, p.getIdPeriodo());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM periodos_academicos WHERE id_periodo = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private List<PeriodoAcademico> consultar(String sql, String parametro) {
        List<PeriodoAcademico> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setString(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar los periodos.", ex);
        }
        return lista;
    }

    private PeriodoAcademico mapear(ResultSet rs) throws SQLException {
        PeriodoAcademico p = new PeriodoAcademico();
        p.setIdPeriodo(rs.getInt("id_periodo"));
        p.setNombre(rs.getString("nombre"));
        p.setFechaInicio(rs.getDate("fecha_inicio"));
        p.setFechaFin(rs.getDate("fecha_fin"));
        p.setEstado(rs.getString("estado"));
        return p;
    }
}
