package universidad.dao;

import java.sql.SQLException;
import java.util.List;
import universidad.modelo.Cuenta;
import universidad.modelo.Usuario;

public interface UsuarioDAO {

    Usuario login(String username, String password);

    List<Cuenta> listar();

    Cuenta leerPorId(int idUsuario);

    /** Crea la cuenta con la contraseña hasheada (PBKDF2). */
    Cuenta crear(Cuenta cuenta) throws SQLException;

    /** Cambia usuario, rol, vínculo y estado; la contraseña se ignora si viene vacía. */
    int actualizar(Cuenta cuenta) throws SQLException;

    /** Restablece la contraseña escribiendo su hash. */
    int cambiarPassword(int idUsuario, String nuevaPassword) throws SQLException;

    int eliminar(int idUsuario) throws SQLException;

    boolean existeUsername(String username);
}
