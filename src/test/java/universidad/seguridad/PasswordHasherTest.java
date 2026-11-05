package universidad.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guardar la clave sin guardarla: PasswordHasher le saca un hash con PBKDF2, una función lenta a
 * propósito para que nadie pueda adivinar la clave aunque le roben la tabla de la base de datos.
 * A la base solo viaja esa huella, nunca la clave escrita. JUnit ejecuta estas comprobaciones.
 */
class PasswordHasherTest {

    @Test
    void hashGeneradoSeVerificaYNoContieneLaContrasena() {
        String hash = PasswordHasher.hash("admin123");
        assertTrue(PasswordHasher.esHash(hash));
        assertFalse(hash.contains("admin123"));
        assertTrue(PasswordHasher.verificar("admin123", hash));
        assertFalse(PasswordHasher.verificar("otra", hash));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintos() {
        // cada hash lleva una mezcla distinta por dentro, por eso nunca se repite la huella
        assertNotEquals(PasswordHasher.hash("clave"), PasswordHasher.hash("clave"));
    }

    @Test
    void textoPlanoNoSeConsideraHashYNoVerifica() {
        // una clave escrita a mano no pasa por hash; solo se compara si alguien lo pide expreso
        assertFalse(PasswordHasher.esHash("admin123"));
        assertFalse(PasswordHasher.verificar("admin123", "admin123"));
        assertTrue(PasswordHasher.coincideTextoPlano("admin123", "admin123"));
        assertFalse(PasswordHasher.coincideTextoPlano("admin124", "admin123"));
    }

    @Test
    void hashCorruptoNoLanzaExcepcion() {
        // una huella mal armada o una clave nula responden "no coincide" en vez de reventar
        assertFalse(PasswordHasher.verificar("x", "pbkdf2_sha256$abc$%%$%%"));
        assertFalse(PasswordHasher.verificar(null, PasswordHasher.hash("x")));
    }
}
