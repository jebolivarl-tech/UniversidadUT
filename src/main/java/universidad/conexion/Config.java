package universidad.conexion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Lee la configuración de la aplicación: primero la variable de entorno y, si no está definida,
 * application-local.properties (en la carpeta de trabajo o en el classpath). Ese archivo nunca se
 * sube a git ni entra en la imagen Docker, así que un PC nuevo puede arrancar solo con variables.
 */
public final class Config {

    private static final Properties CONFIG_LOCAL = cargarConfigLocal();

    private Config() {
    }

    public static String leer(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            valor = CONFIG_LOCAL.getProperty(nombre);
        }
        return valor;
    }

    /** Variable obligatoria: sin ella la aplicación no puede arrancar de forma segura. */
    public static String requerida(String nombre) {
        String valor = leer(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno obligatoria " + nombre
                    + ". Defínala como variable de entorno o en application-local.properties (ver application-local.properties.example).");
        }
        return valor;
    }

    public static String opcional(String nombre, String porDefecto) {
        String valor = leer(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    public static boolean esVerdadero(String nombre) {
        return "true".equalsIgnoreCase(opcional(nombre, ""));
    }

    /** Puerto del servidor web. Vale tanto en HTTP como en el aviso que lee el usuario en pantalla. */
    public static int puerto() {
        String valor = opcional("PORT", "8080");
        try {
            int puerto = Integer.parseInt(valor.trim());
            if (puerto < 1 || puerto > 65535) {
                throw new NumberFormatException();
            }
            return puerto;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("PORT debe ser un número entre 1 y 65535; el valor leído fue \"" + valor + "\".");
        }
    }

    /** Orígenes allow-list de CORS. Vacío = solo el mismo origen, que es lo que necesita localhost. */
    public static List<String> origenesCors() {
        String valor = opcional("CORS_ORIGINS", "");
        if (valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
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
                try (InputStream in = Config.class.getClassLoader().getResourceAsStream("application-local.properties")) {
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
}
