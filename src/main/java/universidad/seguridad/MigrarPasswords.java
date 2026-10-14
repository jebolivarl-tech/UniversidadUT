package universidad.seguridad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import universidad.conexion.Conexion;

/**
 * Herramienta de una sola ejecución: convierte a hash todas las contraseñas que aún estén en texto plano.
 * Uso: mvn exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords
 * (necesita DB_URL, DB_USER y DB_PASSWORD configuradas)
 */
public final class MigrarPasswords {

    private MigrarPasswords() {
    }

    public static void main(String[] args) throws SQLException {
        int migradas = 0;
        try (Connection cx = Conexion.getConexion();
                PreparedStatement select = cx.prepareStatement("SELECT id_usuario, password FROM usuarios");
                PreparedStatement update = cx.prepareStatement("UPDATE usuarios SET password = ? WHERE id_usuario = ?");
                ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                String actual = rs.getString("password");
                if (PasswordHasher.esHash(actual)) {
                    continue;
                }
                update.setString(1, PasswordHasher.hash(actual));
                update.setInt(2, rs.getInt("id_usuario"));
                update.executeUpdate();
                migradas++;
            }
        } finally {
            Conexion.cerrar();
        }
        System.out.println("Contraseñas migradas a hash: " + migradas);
    }
}
