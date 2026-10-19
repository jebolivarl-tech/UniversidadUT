package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Matricula {

    private int idMatricula;
    private int idEstudiante;
    private int idCurso;
    private String periodo;
    private Double nota;

    public Matricula() {
    }

    public Matricula(int idEstudiante, int idCurso, String periodo, Double nota) {
        this.idEstudiante = idEstudiante;
        this.idCurso = idCurso;
        this.periodo = periodo;
        this.nota = nota;
    }

    public int getIdMatricula() { return idMatricula; }
    public void setIdMatricula(int idMatricula) { this.idMatricula = idMatricula; }

    public int getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public int getIdCurso() { return idCurso; }
    public void setIdCurso(int idCurso) { this.idCurso = idCurso; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }
}
