package universidad.conexion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Acceso a MySQL mediante un pool de conexiones (HikariCP).
 * Variables de entorno: DB_URL (opcional), DB_USER y DB_PASSWORD (obligatorias), DB_POOL_SIZE (opcional).
 * Para desarrollo local, si no están definidas se leen de application-local.properties (en la carpeta de
 * trabajo o en el classpath). Ese archivo NO se sube a git ni entra en la imagen Docker.
 * Al cerrar la conexión devuelta (try-with-resources) esta regresa al pool.
 */
public final class Conexion {

    private static final Properties CONFIG_LOCAL = cargarConfigLocal();
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
        config.setJdbcUrl(opcional("DB_URL", "jdbc:mysql://localhost:3306/universidad"));
        config.setUsername(requerida("DB_USER"));
        config.setPassword(requerida("DB_PASSWORD"));
        config.setMaximumPoolSize(Integer.parseInt(opcional("DB_POOL_SIZE", "10")));
        config.setConnectionTimeout(10_000);
        // Permite arrancar la aplicación aunque la BD no esté disponible todavía; fallará solo al consultar.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private static Properties cargarConfigLocal() {
        Properties props = new Properties();
        Path enDisco = Path.of("application-local.properties");
        try {
            if (Files.isRegularFile(enDisco)) {
                try (Reader in = Files.newBufferedReader(enDisco, StandardCharsets.UTF_8)) {
                    props.load(in);
                }
            } else {
                try (InputStream in = Conexion.class.getClassLoader().getResourceAsStream("application-local.properties")) {
                    if (in != null) {
                        // Properties.load(InputStream) decodifica ISO-8859-1; el Reader deja claro que es UTF-8.
                        try (Reader lector = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                            props.load(lector);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer application-local.properties.", ex);
        }
        return props;
    }

    /** Variable de entorno; si no existe, valor de application-local.properties. */
    private static String leer(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            valor = CONFIG_LOCAL.getProperty(nombre);
        }
        return valor;
    }

    private static String requerida(String nombre) {
        String valor = leer(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno obligatoria " + nombre
                    + ". Defínala como variable de entorno o en application-local.properties (ver application-local.properties.example).");
        }
        return valor;
    }

    private static String opcional(String nombre, String porDefecto) {
        String valor = leer(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
