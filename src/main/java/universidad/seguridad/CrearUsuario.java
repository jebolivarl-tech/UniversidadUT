package universidad.seguridad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import universidad.conexion.Conexion;

/**
 * Crea una cuenta de acceso con la contraseña ya convertida a hash.
 * Uso: java -cp target/app.jar universidad.seguridad.CrearUsuario ana estudiante123 ESTUDIANTE 3
 * Los roles PROFESOR y ESTUDIANTE reciben el id de su ficha como cuarto argumento: una cuenta sin
 * ficha no puede iniciar sesión útil y la base de datos la rechaza.
 */
public final class CrearUsuario {

    private static final Set<String> ROLES = Set.of("ADMINISTRATIVO", "PROFESOR", "ESTUDIANTE");

    private CrearUsuario() {
    }

    public static void main(String[] args) throws SQLException {
        if (args.length < 3 || args.length > 4 || !ROLES.contains(args[2])) {
            System.err.println("Uso: CrearUsuario <usuario> <contraseña> <ADMINISTRATIVO|PROFESOR|ESTUDIANTE> [id-ficha]");
            System.exit(1);
        }
        if (args[1].length() < 10) {
            System.err.println("La contraseña debe tener al menos 10 caracteres.");
            System.exit(1);
        }
        boolean administrativa = "ADMINISTRATIVO".equals(args[2]);
        if (administrativa && args.length == 4) {
            System.err.println("La cuenta administrativa no vincula fichas: omita el id.");
            System.exit(1);
        }
        Integer idFicha = administrativa ? null : idFichaObligatorio(args);
        try (Connection cx = Conexion.getConexion()) {
            if (idFicha != null && !existeFicha(cx, args[2], idFicha)) {
                System.err.println("No existe la ficha " + args[2] + " #" + idFicha + ". Cree primero el registro académico.");
                System.exit(1);
            }
            String sql = administrativa
                    ? "INSERT INTO usuarios (username, password, rol, estado) VALUES (?, ?, ?, 1)"
                    : "INSERT INTO usuarios (username, password, rol, " + columnaFicha(args[2])
                            + ", estado) VALUES (?, ?, ?, ?, 1)";
            try (PreparedStatement ps = cx.prepareStatement(sql)) {
                ps.setString(1, args[0]);
                ps.setString(2, PasswordHasher.hash(args[1]));
                ps.setString(3, args[2]);
                if (!administrativa) {
                    ps.setInt(4, idFicha);
                }
                ps.executeUpdate();
            }
            System.out.println("Usuario creado: " + args[0] + " (" + args[2]
                    + (idFicha == null ? ")" : ", ficha " + idFicha + ")"));
        } finally {
            Conexion.cerrar();
        }
    }

    private static Integer idFichaObligatorio(String[] args) {
        if (args.length < 4) {
            System.err.println("El rol " + args[2] + " necesita el id de su ficha (cuarto argumento).");
            System.exit(1);
        }
        try {
            int id = Integer.parseInt(args[3]);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignorado) {
            // un id ilegible y un id fuera de rango se resuelven con el mismo mensaje, el de abajo
        }
        System.err.println("El id de ficha debe ser un número entero positivo.");
        System.exit(1);
        return null;
    }

    /** Columna de `usuarios` que apunta a la ficha académica del rol. */
    private static String columnaFicha(String rol) {
        return "PROFESOR".equals(rol) ? "id_profesor" : "id_estudiante";
    }

    private static boolean existeFicha(Connection cx, String rol, int idFicha) throws SQLException {
        String sql = "PROFESOR".equals(rol)
                ? "SELECT 1 FROM profesores WHERE id_profesor = ?"
                : "SELECT 1 FROM estudiantes WHERE id_estudiante = ?";
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, idFicha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
