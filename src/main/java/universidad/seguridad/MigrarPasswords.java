package universidad.seguridad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import universidad.conexion.Conexion;

/**
 * Barrido de una sola ejecución: recorre la tabla usuarios de MySQL y pasa a hash (la huella irreversible
 * de la contraseña) las claves que todavía están escritas tal como las tecleó la persona.
 * Uso: mvn exec:java -Dexec.mainClass=universidad.seguridad.MigrarPasswords
 * (necesita DB_URL, DB_USER y DB_PASSWORD configuradas)
 *
 * Por qué existe. El login ya convierte en hash una cuenta en el momento en que su dueño entra, pero eso
 * deja sin tocar las que nunca vuelven a iniciar sesión: esas seguirían guardando la clave en texto plano,
 * a la vista de cualquiera que pudiera leer la tabla o exportarla. Esta herramienta las pasa todas de una
 * vez. No repite trabajo: las que ya tienen su huella de PBKDF2 se saltan, así que se puede volver a
 * ejecutar sin dañar a nadie.
 */
public final class MigrarPasswords {

    // De esta clase no se crean objetos: se ejecuta su main una vez y se olvida.
    private MigrarPasswords() {
    }

    public static void main(String[] args) throws SQLException {
        int migradas = 0;
        // La consulta de lectura y la de actualización se preparan una vez y se reutilizan en el recorrido.
        // Al cerrar el bloque, la conexión no se destruye: vuelve al pool de conexiones, el manojo de
        // conexiones a MySQL que la aplicación tiene abiertas y va prestando.
        try (Connection cx = Conexion.getConexion();
                PreparedStatement select = cx.prepareStatement("SELECT id_usuario, password FROM usuarios");
                PreparedStatement update = cx.prepareStatement("UPDATE usuarios SET password = ? WHERE id_usuario = ?");
                ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                String actual = rs.getString("password");
                if (PasswordHasher.esHash(actual)) {
                    continue;
                }
                // Convierte una fila: la huella lleva su sal, así que dos cuentas con la misma clave quedan
                // guardadas de forma distinta.
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
