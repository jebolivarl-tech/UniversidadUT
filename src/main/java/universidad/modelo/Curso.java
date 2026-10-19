package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Curso {

    private int idCurso;
    private String nombre;
    private int creditos;
    private int idCarrera;
    private int idProfesor;

    public Curso() {
    }

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
