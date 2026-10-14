package universidad.seguridad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import universidad.conexion.Conexion;

/**
 * Crea un usuario administrativo/profesor/estudiante con la contraseña ya convertida a hash.
 * Uso: mvn exec:java -Dexec.mainClass=universidad.seguridad.CrearUsuario -Dexec.args="usuario contraseña ADMINISTRATIVO"
 * (los usuarios ESTUDIANTE/PROFESOR deben vincularse después a su id_estudiante / id_profesor).
 */
public final class CrearUsuario {

    private CrearUsuario() {
    }

    public static void main(String[] args) throws SQLException {
        if (args.length != 3 || !Set.of("ADMINISTRATIVO", "PROFESOR", "ESTUDIANTE").contains(args[2])) {
            System.err.println("Uso: CrearUsuario <usuario> <contraseña> <ADMINISTRATIVO|PROFESOR|ESTUDIANTE>");
            System.exit(1);
        }
        if (args[1].length() < 10) {
            System.err.println("La contraseña debe tener al menos 10 caracteres.");
            System.exit(1);
        }
        try (Connection cx = Conexion.getConexion();
                PreparedStatement ps = cx.prepareStatement(
                        "INSERT INTO usuarios (username, password, rol, estado) VALUES (?, ?, ?, 1)")) {
            ps.setString(1, args[0]);
            ps.setString(2, PasswordHasher.hash(args[1]));
            ps.setString(3, args[2]);
            ps.executeUpdate();
            System.out.println("Usuario creado: " + args[0] + " (" + args[2] + ")");
        } finally {
            Conexion.cerrar();
        }
    }
}
