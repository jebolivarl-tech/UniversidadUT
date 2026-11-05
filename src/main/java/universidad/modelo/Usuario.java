package universidad.modelo;

/**
 * Lo que queda de una cuenta después de un inicio de sesión correcto: su nombre, el rol y las fichas
 * académicas que representa. Con esto se arma la sesión, o sea lo que el servidor recuerda de quién entró
 * mientras dura su visita; sin sesión no hay permiso para leer ni escribir.
 *
 * Aquí no viaja la contraseña: en el login solo se compara contra el hash guardado, la huella con la que
 * MySQL guarda la clave en lugar del texto, y el resultado no se copia en este objeto. El formulario de
 * entrada usa una clase aparte para recibir el JSON, justamente para no llenar esta desde la web.
 */
public class Usuario {
    private int idUsuario;
    private String username;
    // En el login este campo queda vacío: la contraseña se usa para comparar y no se guarda aquí.
    private String password;
    private String rol;
    // Puede quedar vacío: una cuenta ADMINISTRATIVO no tiene ficha académica que vincular.
    private Integer idEstudiante;
    private Integer idProfesor;
    // Con estado falso no se puede iniciar sesión; la fila y su historial se conservan.
    private boolean estado;

    // Vacío a propósito: así la arma el DAO, la clase que baja a MySQL con SQL, antes de llenar sus campos.
    public Usuario() {}

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
