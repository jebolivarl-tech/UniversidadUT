package universidad.conexion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Acceso a MySQL mediante un pool de conexiones (HikariCP).
 * Variables de entorno: DB_URL (opcional), DB_USER y DB_PASSWORD (obligatorias), DB_POOL_SIZE (opcional).
 * Al cerrar la conexión devuelta (try-with-resources) esta regresa al pool.
 */
public final class Conexion {

    private static volatile HikariDataSource dataSource;

    private Conexion() {
    }

    public static Connection getConexion() throws SQLException {
        return pool().getConnection();
    }

    public static void cerrar() {
        synchronized (Conexion.class) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

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

    private static HikariDataSource crearPool() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("universidad-pool");
        config.setJdbcUrl(Config.opcional("DB_URL", "jdbc:mysql://localhost:3306/universidad"));
        config.setUsername(Config.requerida("DB_USER"));
        config.setPassword(Config.requerida("DB_PASSWORD"));
        config.setMaximumPoolSize(Integer.parseInt(Config.opcional("DB_POOL_SIZE", "10")));
        config.setConnectionTimeout(10_000);
        // Permite arrancar la aplicación aunque la BD no esté disponible todavía; fallará solo al consultar.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }
}
