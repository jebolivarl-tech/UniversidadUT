package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una asignatura: nombre, cuántos créditos vale y a qué carrera pertenece, con el profesor que suele
 * dictarla. Los créditos pesan en la regla que limita cuántos puede juntar un estudiante en un
 * periodo; el rango permitido lo vigila MySQL, no esta clase.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Curso {

    private int idCurso;
    private String nombre;
    private int creditos;
    private int idCarrera;
    private int idProfesor;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Curso() {
    }

    /** Alta: el id lo pone la base de datos al insertar. */
    public Curso(String nombre, int creditos, int idCarrera, int idProfesor) {
        this.nombre = nombre;
        this.creditos = creditos;
        this.idCarrera = idCarrera;
        this.idProfesor = idProfesor;
    }

    public int getIdCurso() { return idCurso; }
    public void setIdCurso(int idCurso) { this.idCurso = idCurso; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }

    public int getIdProfesor() { return idProfesor; }
    public void setIdProfesor(int idProfesor) { this.idProfesor = idProfesor; }
}
