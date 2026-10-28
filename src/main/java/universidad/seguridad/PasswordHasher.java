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
 * Hashing de contraseñas con PBKDF2-HMAC-SHA256 (incluido en el JDK, sin dependencias).
 * Formato almacenado: pbkdf2_sha256$iteraciones$salt_base64$hash_base64
 */
public final class PasswordHasher {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 600_000;
    private static final int BYTES_SALT = 16;
    private static final int BITS_CLAVE = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Hash ficticio para igualar el tiempo de respuesta cuando el usuario no existe. */
    private static final String HASH_FICTICIO = hash("contrasena-ficticia-no-valida");

    private PasswordHasher() {
    }

    public static String hash(String contrasena) {
        byte[] salt = new byte[BYTES_SALT];
        RANDOM.nextBytes(salt);
        byte[] derivada = derivar(contrasena, salt, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivada);
    }

    /** Cumplir las cuatro partes del formato lo distingue de una contraseña heredada en texto plano. */
    public static boolean esHash(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO + "$") && almacenado.split("\\$").length == 4;
    }

    /** Sin hash válido no hay verificación posible: devuelve false antes que comparar en texto plano. */
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
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Comparación en tiempo constante para contraseñas heredadas en texto plano (solo para migrarlas). */
    public static boolean coincideTextoPlano(String contrasena, String almacenado) {
        if (contrasena == null || almacenado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                contrasena.getBytes(StandardCharsets.UTF_8),
                almacenado.getBytes(StandardCharsets.UTF_8));
    }

    /** Consume el mismo tiempo que una verificación real; se usa cuando el usuario no existe. */
    public static void verificacionFicticia(String contrasena) {
        verificar(contrasena == null ? "" : contrasena, HASH_FICTICIO);
    }

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
