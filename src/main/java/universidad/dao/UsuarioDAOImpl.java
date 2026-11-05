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

/**
 * DAO de las cuentas de acceso. Es el único DAO que no sale de una tabla de negocio: decide quién entra,
 * y el usuario que devuelve es el que la aplicación guarda en la sesión, que es el recuerdo que el servidor
 * le deja al navegador de quién está conectado.
 * La contraseña jamás se guarda ni se compara en claro: se guarda su hash, la huella que sale de mezclarla
 * con un valor aleatorio cientos de miles de veces. De la huella no se puede volver a la contraseña, y dos
 * personas con la misma clave guardan huellas distintas.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    /**
     * Comprueba las credenciales y devuelve null si algo no cuadra, sin decir qué fue: así quien prueba
     * desde fuera no averigua si el nombre existe o si falló la contraseña.
     * Dos detalles que el profesor suele preguntar:
     * - El estado de la cuenta filtra en la propia consulta. Una cuenta desactivada no entra aunque la
     *   contraseña sea la buena.
     * - Las contraseñas se guardan como hash PBKDF2. Si la fila todavía trae una contraseña heredada en
     *   texto plano, se compara en texto y, al coincidir, se reescribe como hash: la migración ocurre sola,
     *   en el primer acceso válido.
     */
    @Override
    public Usuario login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND estado = 1";
        try (Connection conexion = Conexion.getConexion();
                PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Nombre no encontrado: se pierde el mismo tiempo que en una comprobación real, para que
                    // la rapidez de la respuesta no delate qué usuarios sí existen.
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
                // Un administrativo no tiene ficha de estudiante ni de profesor: esos dos campos vienen vacíos,
                // y por eso se pregunta si el número leído fue realmente un hueco.
                int idEst = rs.getInt("id_estudiante");
                u.setIdEstudiante(rs.wasNull() ? null : idEst);
                int idProf = rs.getInt("id_profesor");
                u.setIdProfesor(rs.wasNull() ? null : idProf);
                u.setEstado(rs.getBoolean("estado"));

                if (heredada) {
                    // Se aprovecha la misma conexión que aún está abierta, antes de devolver el usuario.
                    migrarPassword(conexion, u.getIdUsuario(), password);
                }
                return u;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible validar el usuario.", e);
        }
    }

    // Reescribe la contraseña heredada por su hash, calculado con una sal nueva. Desde este momento la fila
    // ya no tiene el texto en claro por ningún lado.
    private void migrarPassword(Connection conexion, int idUsuario, String password) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE usuarios SET password = ? WHERE id_usuario = ?")) {
            ps.setString(1, PasswordHasher.hash(password));
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    // Las consultas de administración de cuentas usan esta lista de columnas. Fíjese que la contraseña no
    // está: ninguna pantalla la necesita y mostrarla sería un riesgo.
    private static final String COLUMNAS_CUENTA =
            "SELECT id_usuario, username, rol, id_estudiante, id_profesor, estado FROM usuarios";

    // Ordenadas por usuario para que la lista de cuentas sea estable entre visitas.
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

    // Da de alta una cuenta. Lo que se manda a MySQL en el campo password es la huella, nunca lo que el
    // usuario tecleó; ese texto solo vive durante la petición y jamás sale en una respuesta.
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

    // Esta orden no menciona la contraseña: editar una cuenta nunca la pisa. Para eso está cambiarPassword.
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

    // Restablecer una contraseña: se calcula la huella de la nueva y se reemplaza la guardada. Nadie, ni
    // quien la puso, la vuelve a leer.
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

    // Solo interesa saber si el nombre está tomado, así que la consulta pide un número fijo y se corta con
    // LIMIT 1: MySQL deja de buscar en cuanto encuentra la primera fila.
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

    // Escribe un hueco en la columna cuando el valor viene vacío. La cuenta de un administrativo va vacía
    // de las dos fichas; la de un estudiante deja vacía la del profesor, y al revés.
    private static void ponerEntero(PreparedStatement ps, int posicion, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(posicion, Types.INTEGER);
        } else {
            ps.setInt(posicion, valor);
        }
    }

    // La fila leída se convierte en Cuenta. El estado es un número 1 o 0 en MySQL y aquí sale como
    // verdadero o falso.
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
