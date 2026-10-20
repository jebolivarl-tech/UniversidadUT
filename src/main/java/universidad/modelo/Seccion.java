package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Grupo de un curso en un periodo: quién lo dicta, dónde y cuántos caben. Los tres últimos campos
 * (nombreCurso, nombrePeriodo, cuposOcupados) los aporta la consulta y son de solo lectura.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Seccion {

    private int idSeccion;
    private int idCurso;
    private int idProfesor;
    private int idPeriodo;
    private String aula;
    private int cupoMaximo;
    private String nombreCurso;
    private String nombrePeriodo;
    private int cuposOcupados;

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

    public String getNombreCurso() { return nombreCurso; }

    /** Solo salida: los tres los aporta la consulta, el cliente no los fija. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public void setNombreCurso(String nombreCurso) { this.nombreCurso = nombreCurso; }

    public String getNombrePeriodo() { return nombrePeriodo; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public void setNombrePeriodo(String nombrePeriodo) { this.nombrePeriodo = nombrePeriodo; }

    public int getCuposOcupados() { return cuposOcupados; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public void setCuposOcupados(int cuposOcupados) { this.cuposOcupados = cuposOcupados; }

    /** Texto que identifica la sección en combos y reportes. */
    @JsonIgnore
    public String describir() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombreCurso == null ? "sección " + idSeccion : nombreCurso);
        if (aula != null && !aula.isBlank()) {
            sb.append(" · ").append(aula);
        }
        if (nombrePeriodo != null) {
            sb.append(" · ").append(nombrePeriodo);
        }
        return sb.append(" (").append(cuposOcupados).append('/').append(cupoMaximo).append(')').toString();
    }
}
