package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Seccion {

    private int idSeccion;
    private int idCurso;
    private int idProfesor;
    private int idPeriodo;
    private String aula;
    private int cupoMaximo;

    public Seccion() {
    }

    public Seccion(int idCurso, int idProfesor, int idPeriodo, String aula, int cupoMaximo) {
        this.idCurso = idCurso;
        this.idProfesor = idProfesor;
        this.idPeriodo = idPeriodo;
        this.aula = aula;
        this.cupoMaximo = cupoMaximo;
    }

    public int getIdSeccion() { return idSeccion; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }

    public int getIdCurso() { return idCurso; }
    public void setIdCurso(int idCurso) { this.idCurso = idCurso; }

    public int getIdProfesor() { return idProfesor; }
    public void setIdProfesor(int idProfesor) { this.idProfesor = idProfesor; }

    public int getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(int idPeriodo) { this.idPeriodo = idPeriodo; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public int getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(int cupoMaximo) { this.cupoMaximo = cupoMaximo; }
}
