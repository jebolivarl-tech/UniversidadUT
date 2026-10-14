package universidad.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import universidad.conexion.Conexion;
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
}
