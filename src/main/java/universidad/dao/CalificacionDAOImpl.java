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
import universidad.modelo.Calificacion;

public class CalificacionDAOImpl implements CalificacionDAO {

    private static final String INSERT = """
            INSERT INTO calificaciones (id_estudiante, id_seccion, nota_parcial1, nota_parcial2, nota_examen_final, estado)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE calificaciones SET id_estudiante = ?, id_seccion = ?, nota_parcial1 = ?, nota_parcial2 = ?,
            nota_examen_final = ?, estado = ? WHERE id_calificacion = ?
            """;

    @Override
    public List<Calificacion> listarTodos() {
        return consultar("SELECT * FROM calificaciones ORDER BY id_seccion, id_estudiante", null);
    }

    @Override
    public List<Calificacion> listarPorSeccion(int idSeccion) {
        return consultar("SELECT * FROM calificaciones WHERE id_seccion = ? ORDER BY id_estudiante", idSeccion);
    }

    @Override
    public List<Calificacion> listarPorEstudiante(int idEstudiante) {
        return consultar("SELECT * FROM calificaciones WHERE id_estudiante = ? ORDER BY id_seccion", idEstudiante);
    }

    @Override
    public Calificacion leerPorEstudianteYSeccion(int idEstudiante, int idSeccion) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(
                     "SELECT * FROM calificaciones WHERE id_estudiante = ? AND id_seccion = ?")) {
            ps.setInt(1, idEstudiante);
            ps.setInt(2, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la calificación.", ex);
        }
    }

    @Override
    public Calificacion leerPorId(int id) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT * FROM calificaciones WHERE id_calificacion = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la calificación.", ex);
        }
    }

    @Override
    public int registrar(Calificacion c) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            int posicion = escribirCuerpo(ps, c);
            ps.setString(posicion, estado(c));
            int filas = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdCalificacion(keys.getInt(1));
                }
            }
            return filas;
        }
    }

    @Override
    public int actualizar(Calificacion c) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(UPDATE)) {
            int posicion = escribirCuerpo(ps, c);
            ps.setString(posicion, estado(c));
            ps.setInt(posicion + 1, c.getIdCalificacion());
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM calificaciones WHERE id_calificacion = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /** El estado no lo decide el cliente: se deriva de las tres notas (ver Calificacion). */
    private static String estado(Calificacion c) {
        return Calificacion.estado(c.getNotaParcial1(), c.getNotaParcial2(), c.getNotaExamenFinal());
    }

    private static int escribirCuerpo(PreparedStatement ps, Calificacion c) throws SQLException {
        ps.setInt(1, c.getIdEstudiante());
        ps.setInt(2, c.getIdSeccion());
        ponerNota(ps, 3, c.getNotaParcial1());
        ponerNota(ps, 4, c.getNotaParcial2());
        ponerNota(ps, 5, c.getNotaExamenFinal());
        return 6;
    }

    private static void ponerNota(PreparedStatement ps, int posicion, Double nota) throws SQLException {
        if (nota == null) {
            ps.setNull(posicion, Types.DECIMAL);
        } else {
            ps.setDouble(posicion, nota);
        }
    }

    private List<Calificacion> consultar(String sql, Integer parametro) {
        List<Calificacion> lista = new ArrayList<>();
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
            throw new IllegalStateException("No fue posible listar las calificaciones.", ex);
        }
        return lista;
    }

    private Calificacion mapear(ResultSet rs) throws SQLException {
        Calificacion c = new Calificacion();
        c.setIdCalificacion(rs.getInt("id_calificacion"));
        c.setIdEstudiante(rs.getInt("id_estudiante"));
        c.setIdSeccion(rs.getInt("id_seccion"));
        c.setNotaParcial1(leerNota(rs, "nota_parcial1"));
        c.setNotaParcial2(leerNota(rs, "nota_parcial2"));
        c.setNotaExamenFinal(leerNota(rs, "nota_examen_final"));
        c.setPromedioFinal(leerNota(rs, "promedio_final"));
        c.setEstado(rs.getString("estado"));
        return c;
    }

    private static Double leerNota(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }
}
