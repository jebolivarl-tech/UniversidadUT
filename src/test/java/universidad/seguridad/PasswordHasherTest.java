package universidad.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
        assertNotEquals(PasswordHasher.hash("clave"), PasswordHasher.hash("clave"));
    }

    @Test
    void textoPlanoNoSeConsideraHashYNoVerifica() {
        assertFalse(PasswordHasher.esHash("admin123"));
        assertFalse(PasswordHasher.verificar("admin123", "admin123"));
        assertTrue(PasswordHasher.coincideTextoPlano("admin123", "admin123"));
        assertFalse(PasswordHasher.coincideTextoPlano("admin124", "admin123"));
    }

    @Test
    void hashCorruptoNoLanzaExcepcion() {
        assertFalse(PasswordHasher.verificar("x", "pbkdf2_sha256$abc$%%$%%"));
        assertFalse(PasswordHasher.verificar(null, PasswordHasher.hash("x")));
    }
}
