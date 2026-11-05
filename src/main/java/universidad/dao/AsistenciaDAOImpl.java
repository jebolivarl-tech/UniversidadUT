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

/**
 * DAO de la asistencia: la pieza que habla con MySQL para guardar y leer los controles de un grupo.
 * DAO significa objeto de acceso a datos; el resto del programa no escribe SQL, solo llama estos
 * métodos. JDBC es la librería de Java que hace esa conversación con la base.
 * Las conexiones salen del pool de conexiones ( Conexion.getConexion ), un manojo de conexiones ya
 * abiertas que se prestan y se devuelven. Con el bloque try entre paréntesis el cierre es automático:
 * al salir, la conexión regresa al pool sin que nadie lo escriba.
 * Cada consulta lleva signos de interrogación en los datos y los valores se entregan aparte, así que
 * MySQL nunca confunde lo que alguien escribió en un formulario con una orden.
 */
public class AsistenciaDAOImpl implements AsistenciaDAO {

    // Todo el historial de la universidad, lo más reciente primero.
    @Override
    public List<Asistencia> listarTodos() {
        return consultar("SELECT * FROM asistencias ORDER BY fecha DESC, id_asistencia", new Object[0]);
    }

    @Override
    public List<Asistencia> listarPorSeccion(int idSeccion) {
        return consultar("SELECT * FROM asistencias WHERE id_seccion = ? ORDER BY fecha DESC, id_asistencia", idSeccion);
    }

    // Los controles de un grupo en un día: la pantalla de pase de lista. La base no deja repetir
    // estudiante, grupo y fecha, así que aquí cada renglón es una persona distinta.
    @Override
    public List<Asistencia> listarPorSeccionYFecha(int idSeccion, Date fecha) {
        return consultar("SELECT * FROM asistencias WHERE id_seccion = ? AND fecha = ? ORDER BY id_asistencia", idSeccion, fecha);
    }

    @Override
    public List<Asistencia> listarPorEstudiante(int idEstudiante) {
        return consultar("SELECT * FROM asistencias WHERE id_estudiante = ? ORDER BY fecha DESC, id_asistencia", idEstudiante);
    }

    // Toda la asistencia de uno en un grupo. El servicio cuenta aquí las faltas para la pérdida del
    // semestre, por eso se necesita la lista completa y no solo un número.
    @Override
    public List<Asistencia> listarPorEstudianteYSeccion(int idEstudiante, int idSeccion) {
        return consultar("SELECT * FROM asistencias WHERE id_estudiante = ? AND id_seccion = ? ORDER BY fecha",
                idEstudiante, idSeccion);
    }

    // Cuántos controles de un grupo están en un estado (ASISTIO, FALTO, TARDANZA, JUSTIFICADO).
    @Override
    public int contarPorSeccionYEstado(int idSeccion, String estado) {
        String sql = "SELECT COUNT(*) FROM asistencias WHERE id_seccion = ? AND estado = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            ps.setString(2, estado);
            try (ResultSet rs = ps.executeQuery()) {
                // El recuento siempre viene en una fila; el 0 es por si la respuesta llega vacía.
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible contar las asistencias.", ex);
        }
    }

    // Una sola fila por su número. Devuelve null si no existe, y quien llamó arma el mensaje.
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

    // El número de la fila lo pone MySQL, no Java. Al guardar se pide ese número recién asignado y se
    // copia al objeto; así la pantalla que creó la asistencia sabe con qué id sigue trabajando.
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

    // Los cuatro datos se reescriben y el id va al final, solo para señalar qué fila cambia.
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

    // Devuelve cuántas filas se borraron: 1 si existía y 0 si ya no estaba.
    @Override
    public int eliminar(int id) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM asistencias WHERE id_asistencia = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Los listar comparten este método: le pasan la consulta y los valores en el orden de los signos de
    // interrogación. Las fechas se mandan como fecha y los demás valores como número.
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
            // Un fallo no se disfraza de lista vacía: la excepción sube para que nadie confunda "no hay"
            // con "no se pudo leer".
            throw new IllegalStateException("No fue posible listar las asistencias.", ex);
        }
        return lista;
    }

    // Convierte la fila que llegó de MySQL en un objeto Asistencia. Los textos entre comillas son los
    // nombres exactos de las columnas.
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
