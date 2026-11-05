package universidad.conexion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * De aquí salen todas las conexiones a MySQL, la base de datos del proyecto. En vez de abrir una
 * nueva en cada pedido, se reutiliza un pool de conexiones: un grupo pequeño de conexiones ya
 * abiertas que va pasando de mano en mano. Abrir una conexión cuesta tiempo, y con el pool ese costo
 * se paga una sola vez. El pool lo arma HikariCP, que habla con MySQL por JDBC, la forma estándar de
 * mandar SQL desde Java.
 *
 * Los datos se los pasa Config: DB_URL (opcional), DB_USER y DB_PASSWORD (obligatorias) y
 * DB_POOL_SIZE (opcional). Cuando el código que pidió una conexión la cierra, la conexión no se
 * destruye: regresa al pool para que la use el siguiente.
 */
public final class Conexion {

    // Aquí queda guardado el pool, que se crea una sola vez. Lleva volatile en su declaración para que
    // un hilo que llega después vea el pool ya armado y no intente construir un segundo.
    private static volatile HikariDataSource dataSource;

    // Nadie crea objetos de esta clase: todo se usa llamando sus métodos de clase.
    private Conexion() {
    }

    // Los DAO, que son las clases que leen y guardan los datos, piden la conexión por aquí: el pool
    // les entrega una libre en vez de abrir una nueva.
    public static Connection getConexion() throws SQLException {
        return pool().getConnection();
    }

    /** Se llama al apagar el servidor (ver el gancho de apagado en ControladorWeb) para soltar el pool. */
    public static void cerrar() {
        synchronized (Conexion.class) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

    /**
     * Entrega el pool ya armado. Primero mira la variable sin bloquear, que es lo rápido; solo si la
     * ve vacía entra al bloqueo y vuelve a mirar, por si otro hilo lo armó mientras esperaba. Así el
     * pool se crea la primera vez que se necesita y nunca se duplica.
     */
    private static HikariDataSource pool() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (Conexion.class) {
                local = dataSource;
                if (local == null) {
                    local = crearPool();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    /** Arma el pool con lo que venga de Config. Solo se ejecuta la primera vez que se pide una conexión. */
    private static HikariDataSource crearPool() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("universidad-pool");
        config.setJdbcUrl(Config.opcional("DB_URL", "jdbc:mysql://localhost:3306/universidad"));
        // El usuario y la contraseña de la base vienen de variables de entorno; en el código no hay ninguno escrito.
        config.setUsername(Config.requerida("DB_USER"));
        config.setPassword(Config.requerida("DB_PASSWORD"));
        // Cuántas conexiones pueden estar abiertas a la vez; se limita para no dejar a MySQL sin recursos.
        config.setMaximumPoolSize(Integer.parseInt(Config.opcional("DB_POOL_SIZE", "10")));
        // Si a los 10 segundos no hay ninguna conexión libre, se devuelve un fallo en vez de dejar esperando al usuario.
        config.setConnectionTimeout(10_000);
        // Con -1 el servidor prende aunque MySQL esté apagado: el fallo sale recién cuando alguien consulta,
        // así que las páginas siguen abriendo mientras la base de datos se levanta.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }
}
