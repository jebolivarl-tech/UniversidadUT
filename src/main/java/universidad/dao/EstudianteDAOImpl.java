package universidad.dao;

import universidad.conexion.Conexion;
import universidad.modelo.Estudiante;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de los estudiantes: la ficha personal (nombres, apellidos, correo, fecha de nacimiento) y el número
 * de la carrera a la que pertenece. La carrera se guarda como referencia, no copiando su nombre, para que
 * al cambiarla no queden fichas desactualizadas.
 * Cada método abre su propia conexión tomada del pool de conexiones y la suelta al terminar el bloque. El
 * SQL siempre va con signos de interrogación y los valores se entregan por separado: lo que un usuario
 * escriba en el formulario jamás se ejecuta como orden.
 */
public class EstudianteDAOImpl implements EstudianteDAO {

    // Estudiantes de toda la universidad. Se usa para el listado general y para traducir ids por nombre.
    @Override
    public List<Estudiante> listarTodos() {
        List<Estudiante> lista = new ArrayList<>();
        String sql = "SELECT * FROM estudiantes";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearEstudiante(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar los estudiantes.", ex);
        }
        return lista;
    }

    // Sin coincidencia devuelve null: el llamador arma el mensaje, aquí no se inventa un estudiante vacío.
    @Override
    public Estudiante leerPorId(int id) {
        String sql = "SELECT * FROM estudiantes WHERE id_estudiante = ?";
        Estudiante est = null;

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    est = mapearEstudiante(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar el estudiante.", ex);
        }
        return est;
    }

    // El id lo numera MySQL con la columna que se va sola. Después de guardar se recupera ese número y se
    // lo queda el objeto, para que la pantalla siga trabajando con el estudiante recién creado. El correo
    // admite varios huecos vacíos, pero la base no deja dos correos repetidos.
    @Override
    public int registrar(Estudiante e) throws SQLException {
        String sql = "INSERT INTO estudiantes (nombre, apellido, email, fecha_nacimiento, id_carrera) VALUES (?, ?, ?, ?, ?)";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getApellido());
            ps.setString(3, e.getEmail());
            ps.setDate(4, e.getFechaNacimiento());
            ps.setInt(5, e.getIdCarrera());
            int filasAfectadas = ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setIdEstudiante(keys.getInt(1));
                }
            }

            return filasAfectadas;
        }
    }

    // Cinco datos nuevos y el id al final. La fecha que quedó sin llenar viaja como hueco, no como una
    // fecha inventada.
    @Override
    public int actualizar(Estudiante e) throws SQLException {
        String sql = "UPDATE estudiantes SET nombre=?, apellido=?, email=?, fecha_nacimiento=?, id_carrera=? WHERE id_estudiante=?";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getApellido());
            ps.setString(3, e.getEmail());
            ps.setDate(4, e.getFechaNacimiento());
            ps.setInt(5, e.getIdCarrera());
            ps.setInt(6, e.getIdEstudiante());
            return ps.executeUpdate();
        }
    }

    // Al borrar a un estudiante se van detrás sus matrículas, notas y controles: eso lo arregla la base,
    // que ató esas filas a esta ficha. Si tiene cuenta de acceso, la cuenta también desaparece.
    @Override
    public int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM estudiantes WHERE id_estudiante = ?";

        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Lee la fila que devolvió MySQL y la convierte en objeto Estudiante. Los textos entre comillas son
    // los nombres de las columnas, escritos tal cual están en la base.
    private Estudiante mapearEstudiante(ResultSet rs) throws SQLException {
        Estudiante e = new Estudiante();
        e.setIdEstudiante(rs.getInt("id_estudiante"));
        e.setNombre(rs.getString("nombre"));
        e.setApellido(rs.getString("apellido"));
        e.setEmail(rs.getString("email"));
        e.setFechaNacimiento(rs.getDate("fecha_nacimiento"));
        e.setIdCarrera(rs.getInt("id_carrera"));
        return e;
    }
}