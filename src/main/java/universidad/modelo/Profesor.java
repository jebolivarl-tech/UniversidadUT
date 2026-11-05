package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * La ficha de un profesor: nombre, apellido, correo y la facultad a la que pertenece.
 * El correo es único en MySQL y puede quedar vacío; la facultad se guarda por su id.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profesor {

    private int idProfesor;
    private String nombre;
    private String apellido;
    private String email;
    private int idFacultad;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Profesor() {
    }

    /** Alta: el id lo pone la base de datos. */
    public Profesor(String nombre, String apellido, String email, int idFacultad) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.idFacultad = idFacultad;
    }

    public int getIdProfesor() { return idProfesor; }
    public void setIdProfesor(int idProfesor) { this.idProfesor = idProfesor; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getIdFacultad() { return idFacultad; }
    public void setIdFacultad(int idFacultad) { this.idFacultad = idFacultad; }
}
