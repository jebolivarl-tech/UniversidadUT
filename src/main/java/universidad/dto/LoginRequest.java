package universidad.dto;

/** Cuerpo JSON de POST /api/login (evita deserializar directamente la entidad Usuario). */
public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest() {
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
