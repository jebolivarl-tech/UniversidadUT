package universidad.dao;
import universidad.modelo.Usuario;

public interface UsuarioDAO {
    Usuario login(String username, String password);
}
