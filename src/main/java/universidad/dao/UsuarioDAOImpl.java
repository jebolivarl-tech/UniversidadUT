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
import universidad.modelo.Cuenta;
import universidad.modelo.Usuario;
import universidad.seguridad.PasswordHasher;

public class UsuarioDAOImpl implements UsuarioDAO {

    /**
     * Valida credenciales. Las contraseñas se verifican contra un hash PBKDF2. Si la cuenta aún tiene una
     * contraseña heredada en texto plano y esta coincide, se migra a hash automáticamente.
     */
    @Override
    public Usuario login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND estado = 1";
        try (Connection conexion = Conexion.getConexion();
                PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    PasswordHasher.verificacionFicticia(password);
                    return null;
                }

                String almacenada = rs.getString("password");
                boolean heredada = !PasswordHasher.esHash(almacenada);
                boolean valida = heredada
                        ? PasswordHasher.coincideTextoPlano(password, almacenada)
                        : PasswordHasher.verificar(password, almacenada);
                if (!valida) {
                    return null;
                }

                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setUsername(rs.getString("username"));
                u.setRol(rs.getString("rol"));
                int idEst = rs.getInt("id_estudiante");
                u.setIdEstudiante(rs.wasNull() ? null : idEst);
                int idProf = rs.getInt("id_profesor");
                u.setIdProfesor(rs.wasNull() ? null : idProf);
                u.setEstado(rs.getBoolean("estado"));

                if (heredada) {
                    migrarPassword(conexion, u.getIdUsuario(), password);
                }
                return u;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible validar el usuario.", e);
        }
    }

    private void migrarPassword(Connection conexion, int idUsuario, String password) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE usuarios SET password = ? WHERE id_usuario = ?")) {
            ps.setString(1, PasswordHasher.hash(password));
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    private static final String COLUMNAS_CUENTA =
            "SELECT id_usuario, username, rol, id_estudiante, id_profesor, estado FROM usuarios";

    @Override
    public List<Cuenta> listar() {
        List<Cuenta> lista = new ArrayList<>();
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(COLUMNAS_CUENTA + " ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCuenta(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible listar las cuentas.", ex);
        }
        return lista;
    }

    @Override
    public Cuenta leerPorId(int idUsuario) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(COLUMNAS_CUENTA + " WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearCuenta(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la cuenta.", ex);
        }
    }

    @Override
    public Cuenta crear(Cuenta cuenta) throws SQLException {
        String sql = "INSERT INTO usuarios (username, password, rol, id_estudiante, id_profesor, estado) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cuenta.getUsername());
            ps.setString(2, PasswordHasher.hash(cuenta.getPassword()));
            ps.setString(3, cuenta.getRol());
            ponerEntero(ps, 4, cuenta.getIdEstudiante());
            ponerEntero(ps, 5, cuenta.getIdProfesor());
            ps.setBoolean(6, cuenta.isEstado());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    cuenta.setIdUsuario(keys.getInt(1));
                }
            }
            return cuenta;
        }
    }

    @Override
    public int actualizar(Cuenta cuenta) throws SQLException {
        String sql = "UPDATE usuarios SET username = ?, rol = ?, id_estudiante = ?, id_profesor = ?, estado = ? WHERE id_usuario = ?";
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, cuenta.getUsername());
            ps.setString(2, cuenta.getRol());
            ponerEntero(ps, 3, cuenta.getIdEstudiante());
            ponerEntero(ps, 4, cuenta.getIdProfesor());
            ps.setBoolean(5, cuenta.isEstado());
            ps.setInt(6, cuenta.getIdUsuario());
            return ps.executeUpdate();
        }
    }

    @Override
    public int cambiarPassword(int idUsuario, String nuevaPassword) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("UPDATE usuarios SET password = ? WHERE id_usuario = ?")) {
            ps.setString(1, PasswordHasher.hash(nuevaPassword));
            ps.setInt(2, idUsuario);
            return ps.executeUpdate();
        }
    }

    @Override
    public int eliminar(int idUsuario) throws SQLException {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("DELETE FROM usuarios WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate();
        }
    }

    @Override
    public boolean existeUsername(String username) {
        try (Connection cx = Conexion.getConexion();
             PreparedStatement ps = cx.prepareStatement("SELECT 1 FROM usuarios WHERE username = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No fue posible consultar la cuenta.", ex);
        }
    }

    private static void ponerEntero(PreparedStatement ps, int posicion, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicion, Types.INTEGER);
        } else {
            ps.setInt(posicion, valor);
        }
    }

    private Cuenta mapearCuenta(ResultSet rs) throws SQLException {
        Cuenta c = new Cuenta();
        c.setIdUsuario(rs.getInt("id_usuario"));
        c.setUsername(rs.getString("username"));
        c.setRol(rs.getString("rol"));
        int idEstudiante = rs.getInt("id_estudiante");
        c.setIdEstudiante(rs.wasNull() ? null : idEstudiante);
        int idProfesor = rs.getInt("id_profesor");
        c.setIdProfesor(rs.wasNull() ? null : idProfesor);
        c.setEstado(rs.getBoolean("estado"));
        return c;
    }
}
