package universidad.dao;

import java.sql.SQLException;
import java.util.List;

public interface CRUD<T> {
    List<T> listarTodos();
    T leerPorId(int id);
    int registrar(T t) throws SQLException;
    int actualizar(T t) throws SQLException;
    int eliminar(int id) throws SQLException;
}
