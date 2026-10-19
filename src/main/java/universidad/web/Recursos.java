package universidad.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Lee (y guarda en memoria) los archivos estáticos empaquetados en el JAR. */
public final class Recursos {

    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, byte[]> CACHE_BINARIO = new ConcurrentHashMap<>();

    private Recursos() {
    }

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
            throw new IllegalStateException("No se pudo cargar el recurso: " + ruta, e);
        }
    }

    /** Recurso binario (PNG, ICO). No pasa por String: decodificarlo como UTF-8 destruiría los bytes. */
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
