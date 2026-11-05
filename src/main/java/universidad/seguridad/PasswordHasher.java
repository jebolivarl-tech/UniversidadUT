package universidad.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Convertir una contraseña en su hash y volver a comprobarla después.
 *
 * El hash es una huella de tamaño fijo que sale de la contraseña. Se puede calcular hacia adelante,
 * pero no se puede invertir para recuperar la clave. Aquí la huella la produce PBKDF2, un algoritmo
 * que trae el propio JDK sin añadir dependencias: mezcla la contraseña con una sal aleatoria cientos
 * de miles de veces. La sal va guardada junto a la huella y resuelve dos cosas. Dos personas con la
 * misma contraseña almacenan huellas distintas, y ya no sirve una tabla de huellas precalculadas para
 * adivinar claves. Las vueltas son caras a propósito: le quitan prisa a quien intente probar millones
 * de contraseñas por segundo.
 *
 * Lo que se escribe en la columna password tiene estas cuatro partes, separadas por signos de dólar:
 *   pbkdf2_sha256$iteraciones$sal_base64$hash_base64
 * Guardar también las vueltas dentro del texto deja subir ese número en el futuro sin romper las cuentas
 * viejas: cada fila lleva las suyas y se verifica con las que tiene escritas.
 */
public final class PasswordHasher {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 600_000;
    private static final int BYTES_SALT = 16;
    private static final int BITS_CLAVE = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * Huella preparada de antemano para poder gastar tiempo verificándola cuando el usuario no existe.
     * Sin ella, una respuesta inmediata delataría que ese nombre no está en la base.
     */
    private static final String HASH_FICTICIO = hash("contrasena-ficticia-no-valida");

    // De esta clase no se crean objetos: sus operaciones se piden por su nombre, como PasswordHasher.hash(...).
    private PasswordHasher() {
    }

    /**
     * Contraseña adentro, texto guardable afuera. Cada llamada tira una sal nueva de 16 bytes, así que
     * la misma contraseña dos veces produce dos textos distintos; ambos verifican igual de bien.
     * La sal y la huella se codifican en Base64 porque son bytes sueltos y en la base de datos hace
     * falta texto.
     */
    public static String hash(String contrasena) {
        byte[] salt = new byte[BYTES_SALT];
        RANDOM.nextBytes(salt);
        byte[] derivada = derivar(contrasena, salt, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivada);
    }

    /**
     * Decide si lo que hay en la base ya es una huella con sus cuatro partes, y no una contraseña heredada
     * escrita en texto plano. Con esa forma se reconoce cuál de los dos caminos hay que tomar al verificar.
     */
    public static boolean esHash(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO + "$") && almacenado.split("\\$").length == 4;
    }

    /**
     * La comparación real: se lee la sal y las vueltas que están escritas junto a la huella guardada, se
     * recalcula la huella con la contraseña que llega y se comparan. No se compara la contraseña, sino el
     * resultado de volver a calcularla. Si algo llega mal formado, se responde false en vez de arriesgar
     * una comparación en texto plano.
     */
    public static boolean verificar(String contrasena, String almacenado) {
        if (contrasena == null || !esHash(almacenado)) {
            return false;
        }
        try {
            String[] partes = almacenado.split("\\$");
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] obtenido = derivar(contrasena, salt, iteraciones);
            // isEqual compara sin delatar en qué posición empezaron a diferir las dos huellas.
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Para las cuentas antiguas que siguen con la contraseña escrita en texto plano. Solo existe mientras
     * se migran: un texto plano no es un hash, y no se verifica comparándolo de manera corriente, para no
     * filtrar información con el tiempo que tarda la comparación.
     */
    public static boolean coincideTextoPlano(String contrasena, String almacenado) {
        if (contrasena == null || almacenado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                contrasena.getBytes(StandardCharsets.UTF_8),
                almacenado.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Se pasa la contraseña por el mismo cálculo de siempre, contra una huella que nunca va a coincidir.
     * Sirve cuando el usuario no existe: la respuesta tarda lo mismo que si existiera, así que por tiempo
     * no se puede averiguar qué nombres de cuenta están en la base.
     */
    public static void verificacionFicticia(String contrasena) {
        verificar(contrasena == null ? "" : contrasena, HASH_FICTICIO);
    }

    /**
     * El paso pesado: PBKDF2 combinando la contraseña, la sal y las vueltas, y devolviendo una huella de
     * 256 bits. La copia de la contraseña se limpia al salir porque PBEKeySpec la guarda en memoria mientras
     * trabaja, y no conviene dejarla ahí tirada.
     */
    private static byte[] derivar(String contrasena, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, iteraciones, BITS_CLAVE);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("No fue posible calcular el hash de la contraseña.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
