package universidad.dao;

import java.sql.SQLException;
import java.util.List;
import universidad.modelo.Cuenta;
import universidad.modelo.Usuario;

/**
 * DAO de la tabla usuarios, donde viven las cuentas de la aplicación. No extiende a
 * {@link CRUD} porque aquí no basta con guardar y listar filas: hace falta el login, que
 * compara la contraseña con el hash guardado. Un hash es un código calculado a partir de la
 * contraseña, generado con PBKDF2, para que el texto real nunca quede en MySQL.
 */
public interface UsuarioDAO {

    /**
     * Valida las credenciales y devuelve al usuario para abrir su sesión en la app (quién está
     * conectado). Solo deja entrar cuentas activas; si falla, devuelve null.
     */
    Usuario login(String username, String password);

    List<Cuenta> listar();

    /** Lee la cuenta sin la contraseña: el SELECT ni siquiera la incluye. */
    Cuenta leerPorId(int idUsuario);

    /** Guarda la cuenta nueva; la contraseña se va al MySQL ya convertida en su hash. */
    Cuenta crear(Cuenta cuenta) throws SQLException;

    /** Cambia usuario, rol, vínculo con estudiante o profesor y el activo/inactivo; la contraseña no la toca. */
    int actualizar(Cuenta cuenta) throws SQLException;

    /** Fija una contraseña nueva: recibe el texto claro y guarda su hash. */
    int cambiarPassword(int idUsuario, String nuevaPassword) throws SQLException;

    int eliminar(int idUsuario) throws SQLException;

    /** Comprueba si el nombre de usuario ya está tomado antes de crear o renombrar una cuenta. */
    boolean existeUsername(String username);
}
