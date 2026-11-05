package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * La cuenta con la que alguien entra al sistema: usuario, contraseña, rol y la ficha académica a la
 * que está ligada (un estudiante, un profesor, o ninguna si es administrativa).
 *
 * Qué pasa con la contraseña. En MySQL no se guarda como la escribió la persona, sino como su hash,
 * una huella calculada con PBKDF2 a partir de una sal aleatoria. La sal hace que dos cuentas con la
 * misma clave almacenen huellas distintas, y la huella no se puede convertir de vuelta en la contraseña.
 *
 * El campo password es de un solo sentido: entra por JSON cuando se crea la cuenta o se cambia la clave,
 * y nunca sale en las respuestas de la API. No es solo el filtro de Jackson: el SELECT, la consulta con la
 * que el DAO lee la tabla, ni siquiera pide la columna password, y al guardar, el DAO escribe el hash, no
 * lo que llegó del formulario. Así la huella vive solo en la fila de la base y en las comparaciones de
 * PasswordHasher; el objeto que recorre la web no la transporta.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cuenta {

    private int idUsuario;
    private String username;

    // Solo se recibe al escribir; al devolver la cuenta en JSON este campo se omite.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String rol;
    // Pueden no llevar nada: una cuenta administrativa no tiene ficha de estudiante ni de profesor.
    private Integer idEstudiante;
    private Integer idProfesor;
    // Cerrada (false) no puede iniciar sesión, pero la fila se conserva con su historial.
    private boolean estado = true;

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // Sirve solo durante la petición: el DAO la convierte en hash al guardar y en las respuestas no aparece.
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
