package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inscripción de un estudiante en una sección de un periodo. El curso y el nombre del periodo se
 * derivan de la sección en el servicio de dominio, nunca los decide el formulario.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Matricula {

    public static final String EN_CURSO = "EN_CURSO";
    public static final String APROBADO = "APROBADO";
    public static final String REPROBADO = "REPROBADO";
    /** Pierde el derecho a evaluación definitiva por inasistencia no justificada. */
    public static final String PERDIDO_POR_FALTA_DE_ASISTENCIA = "PERDIDO_POR_FALTA_DE_ASISTENCIA";

    private int idMatricula;
    private int idEstudiante;
    private int idSeccion;
    private int idCurso;
    private String periodo;
    private Double nota;
    private String estado = EN_CURSO;

    public Matricula() {
    }

    public int getIdMatricula() { return idMatricula; }
    public void setIdMatricula(int idMatricula) { this.idMatricula = idMatricula; }

    public int getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public int getIdSeccion() { return idSeccion; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }

    public int getIdCurso() { return idCurso; }
    public void setIdCurso(int idCurso) { this.idCurso = idCurso; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }

    public String getEstado() { return estado; }

    /** Solo salida: el estado lo deriva el servicio de dominio a partir de nota y asistencia. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public void setEstado(String estado) { this.estado = estado; }
}
