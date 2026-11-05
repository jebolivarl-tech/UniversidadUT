package universidad.modelo;

import java.sql.Date;

/**
 * La ficha de un estudiante: cómo se llama, su correo, cuándo nació y en qué carrera está.
 * El correo es único en MySQL, así que no se pueden duplicar dos fichas con el mismo; la fecha de
 * nacimiento es opcional. La carrera se guarda solo por su id.
 */
public class Estudiante {

    private int idEstudiante;
    private String nombre;
    private String apellido;
    private String email;
    // java.sql.Date: la fecha tal como la manejan JDBC y MySQL, día calendario sin hora.
    private Date fechaNacimiento;
    private int idCarrera;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Estudiante() {}

    /** Alta: el id lo pone la base de datos. */
    public Estudiante(String nombre, String apellido, String email, Date fechaNacimiento, int idCarrera) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.fechaNacimiento = fechaNacimiento;
        this.idCarrera = idCarrera;
    }

    public int getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }
}
