package universidad.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lee los archivos que viajan dentro del JAR (el ejecutable ya empaquetado con las pantallas HTML, la
 * hoja de estilo, el JavaScript y las imágenes) y los deja guardados en memoria. La primera petición
 * paga la lectura del disco; las siguientes salen directas del mapa en memoria, así que abrir el panel
 * no vuelve a leer el mismo archivo cien veces.
 */
public final class Recursos {

    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, byte[]> CACHE_BINARIO = new ConcurrentHashMap<>();

    private Recursos() {
    }

    /** Archivo de texto (HTML, CSS, JS) ya convertido a String, listo para mandar como respuesta HTTP. */
    public static String leer(String ruta) {
        String cacheado = CACHE.get(ruta);
        if (cacheado != null) {
            return cacheado;
        }
        try (InputStream in = Recursos.class.getClassLoader().getResourceAsStream(ruta)) {
            String contenido = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
            CACHE.put(ruta, contenido);
            return contenido;
        } catch (IOException | NullPointerException e) {
            // NullPointer significa que el archivo no está dentro del JAR: falla alto, no se sirve una página vacía.
            throw new IllegalStateException("No se pudo cargar el recurso: " + ruta, e);
        }
    }

    /** Archivo binario (PNG, ICO). Se guarda tal cual de bytes: pasarlo por texto con UTF-8 destrozaría la imagen. */
    public static byte[] leerBytes(String ruta) {
        byte[] cacheado = CACHE_BINARIO.get(ruta);
        if (cacheado != null) {
            return cacheado;
        }
        try (InputStream in = Recursos.class.getClassLoader().getResourceAsStream(ruta)) {
            byte[] contenido = Objects.requireNonNull(in).readAllBytes();
            CACHE_BINARIO.put(ruta, contenido);
            return contenido;
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("No se pudo cargar el recurso: " + ruta, e);
        }
    }
}
