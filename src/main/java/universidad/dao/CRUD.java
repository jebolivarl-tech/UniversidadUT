package universidad.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato común de los DAO de la aplicación. Un DAO es la clase que, con JDBC (el driver que
 * conecta Java con MySQL), ejecuta el SQL de una sola tabla: guardar, listar, buscar por id,
 * modificar y borrar.
 * La T es el objeto Java que representa una fila; en EstudianteDAO, T es un Estudiante.
 * Las implementaciones piden la conexión al pool de conexiones (la reserva de conexiones ya
 * abiertas) que vive en {@link universidad.conexion.Conexion}, y la devuelven al terminar.
 * Ojo con lo que devuelven: leerPorId responde null si el id no existe, y actualizar y eliminar
 * devuelven cuántas filas tocaron, o sea 0 cuando no había nada.
 * Los servicios llaman a estos métodos y la API REST (las rutas que consume el navegador) les da
 * forma de JSON (texto con campos y valores) para mostrarlos en pantalla.
 */
public interface CRUD<T> {
    List<T> listarTodos();
    T leerPorId(int id);
    int registrar(T t) throws SQLException;
    int actualizar(T t) throws SQLException;
    int eliminar(int id) throws SQLException;
}
