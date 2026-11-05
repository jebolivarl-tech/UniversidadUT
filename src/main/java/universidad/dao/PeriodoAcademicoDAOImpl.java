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

/**
 * DAO de los periodos académicos (2026-1, 2026-2...). Un periodo vive cerrado o abierto y de eso depende
 * que se pueda inscribir: por eso el estado se guarda en la fila en vez de calcularlo con la fecha de hoy.
 * Las fechas viajan como fecha de Java y MySQL las guarda como día suelto, sin hora. Cada conexión se toma
 * del pool de conexiones y se devuelve al cerrar el bloque.
 */
public class PeriodoAcademicoDAOImpl implements PeriodoAcademicoDAO {

    @Override
    public List<PeriodoAcademico> listarTodos() {
        // El más reciente arriba; el desplegable del formulario también lo necesita ordenado así.
        return consultar("SELECT * FROM periodos_academicos ORDER BY fecha_inicio DESC", null);
    }

    // ACTIVO, INACTIVO o CERRADO: la columna solo admite esos tres textos, escritos tal cual por quien
    // llama. Con esto el sistema sabe en qué periodo se puede inscribir.
    @Override
    public List<PeriodoAcademico> listarPorEstado(String estado) {
        return consultar("SELECT * FROM periodos_academicos WHERE estado = ? ORDER BY fecha_inicio DESC", estado);
    }

    // Trae null si ese número no está en la tabla; el que llamó arma el aviso que muestra.
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

    // El número lo pone MySQL y se recoge tras guardar. La base además no deja dos periodos con el mismo
    // nombre, porque las matrículas los citan por ese nombre.
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

    // Un periodo con fecha de cierre antes de la de apertura lo rechaza MySQL, no hace falta comprobarlo
    // dos veces desde Java.
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

    // Los grupos del periodo se borran con él, eso lo decidió la base al encadenarlos. Pero si algún grupo
    // ya tiene estudiantes inscritos, MySQL frena el borrado: primero se retiran las matrículas.
    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM periodos_academicos WHERE id_periodo = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // El común de los listar: null significa que la consulta no tiene huecos donde meter valores.
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

    // Copia la fila contestada por MySQL en un objeto PeriodoAcademico, columna por columna.
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
