package universidad.dto;

/**
 * Los dos datos que manda el formulario al POST /api/login, escritos en JSON, el texto con llaves y
 * comas con el que el navegador envía lo que la persona escribió.
 *
 * Se usa una clase apartada en vez de llenar directo la entidad Usuario, porque si el JSON entrara ahí
 * alguien podría mandar también el rol o el identificador y la aplicación los aceptaría. Con esta clase
 * solo entran el nombre de usuario y la contraseña tecleada; lo demás lo decide el servidor.
 *
 * La carpeta dto, donde está esta clase, solo guarda mensajes que entran o salen por la web: datos con
 * su lectura y su escritura, sin ningún trabajo extra.
 */
public class LoginRequest {
    private String username;
    // Llega tal cual se tecleó, y por eso este objeto no se guarda ni se reenvía a ningún lado: solo se
    // usa para comprobarlo contra el hash, la huella que sale de la contraseña. Esa huella, no el texto,
    // es lo único que queda en MySQL, la base de datos del proyecto.
    private String password;

    // Sin argumentos a propósito: la lectura de JSON crea el objeto vacío y luego llena campo por campo.
    public LoginRequest() {
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
