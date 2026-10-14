package universidad.modelo;

public class Usuario {
    private int idUsuario;
    private String username;
    private String password;
    private String rol; // 'ADMINISTRATIVO', 'ESTUDIANTE', 'PROFESOR'
    private Integer idEstudiante; // Usamos Integer (objeto) para permitir valores NULL
    private Integer idProfesor;   // Usamos Integer (objeto) para permitir valores NULL
    private boolean estado;

    // Constructor vacío
    public Usuario() {}

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }

    public Integer getIdProfesor() { return idProfesor; }
    public void setIdProfesor(Integer idProfesor) { this.idProfesor = idProfesor; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
}
