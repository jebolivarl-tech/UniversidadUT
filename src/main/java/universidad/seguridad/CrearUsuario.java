package universidad.seguridad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import universidad.conexion.Conexion;

/**
 * Herramienta de consola para crear una cuenta de acceso, con la contraseña convertida a hash desde el
 * primer momento.
 * Uso: java -cp target/app.jar universidad.seguridad.CrearUsuario ana estudiante123 ESTUDIANTE 3
 *
 * Hace falta porque el primer administrador no puede crearse desde la propia API: para entrar hay que
 * tener cuenta, y para tener cuenta hay que estar dentro. Esta herramienta rompe ese círculo y deja la
 * primera fila de `usuarios` escrita con SQL contra MySQL.
 *
 * La contraseña se convierte en su huella con PBKDF2 y una sal aleatoria, igual que hace la web, así que
 * en la base nunca queda escrita como la tecleó la persona. Los roles PROFESOR y ESTUDIANTE exigen el id de
 * su ficha como cuarto argumento: una cuenta sin ficha no puede iniciar sesión útil. MySQL no sabe expresar
 * esa regla (no admite una comprobación sobre columnas que son llave foránea), así que la vigila la
 * aplicación: aquí y en Validaciones cuando la cuenta se crea desde la API.
 */
public final class CrearUsuario {

    private static final Set<String> ROLES = Set.of("ADMINISTRATIVO", "PROFESOR", "ESTUDIANTE");

    // De esta clase no se crean objetos: se ejecuta su main desde la consola.
    private CrearUsuario() {
    }

    public static void main(String[] args) throws SQLException {
        if (args.length < 3 || args.length > 4 || !ROLES.contains(args[2])) {
            System.err.println("Uso: CrearUsuario <usuario> <contraseña> <ADMINISTRATIVO|PROFESOR|ESTUDIANTE> [id-ficha]");
            System.exit(1);
        }
        // La misma largura mínima que exige la API al crear una cuenta.
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
        // JDBC es el puente de Java a la base de datos. La conexión no se abre de cero: se pide prestada al
        // pool de conexiones, que ya tiene varias listas, y al cerrarla vuelve a él.
        try (Connection cx = Conexion.getConexion()) {
            if (idFicha != null && !existeFicha(cx, args[2], idFicha)) {
                System.err.println("No existe la ficha " + args[2] + " #" + idFicha + ". Cree primero el registro académico.");
                System.exit(1);
            }
            String sql = administrativa
                    ? "INSERT INTO usuarios (username, password, rol, estado) VALUES (?, ?, ?, 1)"
                    : "INSERT INTO usuarios (username, password, rol, " + columnaFicha(args[2])
                            + ", estado) VALUES (?, ?, ?, ?, 1)";
            // Los datos entran por los signos de interrogación, nunca pegados al texto de la consulta. Lo único
            // que se arma con cadenas es el nombre de la columna, y ese nombre lo elige el código entre dos fijos.
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

    /** La ficha académica es obligatoria para los dos roles que sí tienen una. */
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

    /** Antes de insertar, comprobar que la ficha existe; si no, la cuenta apuntaría a una fila que no está. */
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
