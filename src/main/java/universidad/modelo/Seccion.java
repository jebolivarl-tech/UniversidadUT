package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Un grupo de un curso en un periodo: qué asignatura es, quién la dicta, en qué aula y cuántos caben.
 * Los tres últimos campos (nombre del curso, nombre del periodo y cupos ocupados) no se guardan en la
 * fila: los trae la consulta SQL que acompaña al DAO, uniendo las tablas de cursos y periodos y contando
 * las matrículas ya inscritas. Por eso son de solo lectura hacia afuera: si el cliente los mandara, podría
 * inventarse un nombre o fingir que hay cupo libre.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Seccion {

    private int idSeccion;
    private int idCurso;
    private int idProfesor;
    private int idPeriodo;
    private String aula;
    // El tope lo vigila MySQL (entre 1 y 200); los ocupados reales son los que cuenta la consulta.
    private int cupoMaximo;
    private String nombreCurso;
    private String nombrePeriodo;
    private int cuposOcupados;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Seccion() {
    }

    /** Alta: el id lo pone la base de datos; del curso, del profesor y del periodo solo se manda su número. */
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

    /**
     * Texto que identifica la sección en combos y reportes, del tipo "Cálculo · Aula 3 · 2025-2 (18/25)".
     * Se arma al momento y nunca se guarda, por eso va marcada para que no salga en el JSON.
     */
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
