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
 * Saca los valores con los que arranca la aplicación: el puerto, el usuario y la contraseña de MySQL
 * (la base de datos del proyecto), los sitios de afuera que pueden leer sus respuestas, etc. Primero
 * mira la variable de entorno del sistema; si no está puesta, usa application-local.properties, un
 * archivo de texto con líneas nombre=valor que puede estar en la carpeta de trabajo o dentro del
 * propio programa. Ese archivo nunca se sube a git ni entra en la imagen Docker, así que un PC nuevo
 * prende solo con variables de entorno.
 */
public final class Config {

    // El archivo se lee una sola vez, al cargar esta clase; mientras el servidor esté prendido los valores no cambian.
    private static final Properties CONFIG_LOCAL = cargarConfigLocal();

    // Nadie crea objetos de esta clase: solo se usan sus métodos de clase.
    private Config() {
    }

    // Mira las dos fuentes y devuelve la primera que tenga algo; si ninguna tiene, devuelve null.
    public static String leer(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            valor = CONFIG_LOCAL.getProperty(nombre);
        }
        return valor;
    }

    /**
     * Variable obligatoria, como la contraseña de la base de datos. Si falta, se falla apenas alguien la
     * necesita y el mensaje dice cuál es y dónde se puede poner; seguir con un valor adivinado sería peor.
     */
    public static String requerida(String nombre) {
        String valor = leer(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno obligatoria " + nombre
                    + ". Defínala como variable de entorno o en application-local.properties (ver application-local.properties.example).");
        }
        return valor;
    }

    /** Para las variables que pueden faltar: si no vienen puestas, se usa el valor por defecto. */
    public static String opcional(String nombre, String porDefecto) {
        String valor = leer(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    /** Se acepta true escrito como sea; cualquier otro valor, incluida la ausencia, cuenta como falso. */
    public static boolean esVerdadero(String nombre) {
        return "true".equalsIgnoreCase(opcional(nombre, ""));
    }

    /**
     * El puerto por donde entra el tráfico HTTP, o sea el número de la puerta del servidor web. Sirve
     * tanto para prender el servidor como para armar el aviso que la persona lee en pantalla, y así los
     * dos coinciden. Solo se aceptan números entre 1 y 65535, que es el rango que existe.
     */
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

    /**
     * Los sitios de otros servidores a los que les dejamos leer nuestras respuestas. Vienen en
     * CORS_ORIGINS separados por comas. Si la lista queda vacía no se permite ninguno de afuera, y eso
     * alcanza en localhost, porque ahí la misma página es la que pregunta.
     */
    public static List<String> origenesCors() {
        String valor = opcional("CORS_ORIGINS", "");
        if (valor.isBlank()) {
            return List.of();
        }
        // Los espacios y las comas sueltas del texto no arman un origen.
        return Arrays.stream(valor.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Carga application-local.properties una sola vez. Se busca primero en la carpeta donde se ejecuta
     * el programa y, si no está ahí, dentro del propio .jar; así funciona igual en el PC de cada quien
     * y en el contenedor.
     */
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
                        // Load recibe bytes y con bytes asume ISO-8859-1, lo que rompería los acentos del archivo.
                        // Se le pasa un lector que ya sabe leer UTF-8.
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
