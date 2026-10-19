package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Calificación de un estudiante en una sección. El promedio lo calcula MySQL (columna generada,
 * 30 % + 30 % + 40 %) y el estado se deriva en el servidor: EN_CURSO si falta alguna nota,
 * APROBADO con promedio >= 3.0, REPROBADO en caso contrario.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Calificacion {

    public static final String APROBADO = "APROBADO";
    public static final String REPROBADO = "REPROBADO";
    public static final String EN_CURSO = "EN_CURSO";

    public static final double PROMEDIO_APROBATORIO = 3.0;

    private int idCalificacion;
    private int idEstudiante;
    private int idSeccion;
    private Double notaParcial1;
    private Double notaParcial2;
    private Double notaExamenFinal;
    private Double promedioFinal;
    private String estado;

    public Calificacion() {
    }

    public Calificacion(int idEstudiante, int idSeccion, Double notaParcial1, Double notaParcial2, Double notaExamenFinal) {
        this.idEstudiante = idEstudiante;
        this.idSeccion = idSeccion;
        this.notaParcial1 = notaParcial1;
        this.notaParcial2 = notaParcial2;
        this.notaExamenFinal = notaExamenFinal;
    }

    public static double promedio(Double parcial1, Double parcial2, Double examenFinal) {
        return (parcial1 == null ? 0 : parcial1) * 0.3
                + (parcial2 == null ? 0 : parcial2) * 0.3
                + (examenFinal == null ? 0 : examenFinal) * 0.4;
    }

    public static String estado(Double parcial1, Double parcial2, Double examenFinal) {
        if (parcial1 == null || parcial2 == null || examenFinal == null) {
            return EN_CURSO;
        }
        return promedio(parcial1, parcial2, examenFinal) >= PROMEDIO_APROBATORIO ? APROBADO : REPROBADO;
    }

    public int getIdCalificacion() { return idCalificacion; }
    public void setIdCalificacion(int idCalificacion) { this.idCalificacion = idCalificacion; }

    public int getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public int getIdSeccion() { return idSeccion; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }

    public Double getNotaParcial1() { return notaParcial1; }
    public void setNotaParcial1(Double notaParcial1) { this.notaParcial1 = notaParcial1; }

    public Double getNotaParcial2() { return notaParcial2; }
    public void setNotaParcial2(Double notaParcial2) { this.notaParcial2 = notaParcial2; }

    public Double getNotaExamenFinal() { return notaExamenFinal; }
    public void setNotaExamenFinal(Double notaExamenFinal) { this.notaExamenFinal = notaExamenFinal; }

    public Double getPromedioFinal() { return promedioFinal; }

    /** Solo salida: el promedio lo calcula MySQL. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public void setPromedioFinal(Double promedioFinal) { this.promedioFinal = promedioFinal; }

    public String getEstado() { return estado; }

    /** Solo salida: el estado se deriva de las notas, no lo envía el cliente. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public void setEstado(String estado) { this.estado = estado; }
}
