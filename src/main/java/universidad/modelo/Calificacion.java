package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Las tres notas de un estudiante en una sección: dos parciales y el examen final.
 * El promedio no lo decide esta clase. MySQL lo saca solo, en una columna generada que pondera
 * 30 % el primer parcial, 30 % el segundo y 40 % el final, y el estado lo pone ServicioCalificaciones
 * antes de guardar: EN_CURSO mientras falte alguna nota, APROBADO desde 3.0, REPROBADO por debajo, y
 * PERDIDO_POR_FALTA_DE_ASISTENCIA si el estudiante perdió por inasistencia. Por eso ninguno de los dos
 * viaja desde el formulario: si el cliente los eligiera, bastaría escribir "APROBADO" para aprobar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Calificacion {

    public static final String APROBADO = "APROBADO";
    public static final String REPROBADO = "REPROBADO";
    public static final String EN_CURSO = "EN_CURSO";
    /** Sacó nota, pero la inasistencia no justificada le quitó el derecho a la evaluación definitiva. */
    public static final String PERDIDO_POR_FALTA_DE_ASISTENCIA = "PERDIDO_POR_FALTA_DE_ASISTENCIA";

    /** Nota mínima para aprobar, en la escala de 0 a 5. */
    public static final double PROMEDIO_APROBATORIO = 3.0;

    private int idCalificacion;
    private int idEstudiante;
    private int idSeccion;
    // Notas que pueden quedar vacías: un 0 es una nota presentada, no una nota sin digitar.
    private Double notaParcial1;
    private Double notaParcial2;
    private Double notaExamenFinal;
    private Double promedioFinal;
    private String estado = EN_CURSO;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Calificacion() {
    }

    /** Alta: solo las tres notas. El id, el promedio y el estado llegan después, de la base de datos. */
    public Calificacion(int idEstudiante, int idSeccion, Double notaParcial1, Double notaParcial2, Double notaExamenFinal) {
        this.idEstudiante = idEstudiante;
        this.idSeccion = idSeccion;
        this.notaParcial1 = notaParcial1;
        this.notaParcial2 = notaParcial2;
        this.notaExamenFinal = notaExamenFinal;
    }

    /**
     * La misma ponderación que aplica MySQL en su columna generada, escrita también en Java, con las notas
     * sin cargar valiendo 0. Sirve para adelantar el resultado en pantalla y para sacar la nota definitiva
     * de la matrícula; el valor que queda guardado en la base lo calcula MySQL.
     */
    public static double promedio(Double parcial1, Double parcial2, Double examenFinal) {
        return (parcial1 == null ? 0 : parcial1) * 0.3
                + (parcial2 == null ? 0 : parcial2) * 0.3
                + (examenFinal == null ? 0 : examenFinal) * 0.4;
    }

    /** Con alguna nota sin cargar el curso sigue abierto; con las tres, se aprueba desde 3.0. */
    public static String estado(Double parcial1, Double parcial2, Double examenFinal) {
        if (parcial1 == null || parcial2 == null || examenFinal == null) {
            return EN_CURSO;
        }
        return promedio(parcial1, parcial2, examenFinal) >= PROMEDIO_APROBATORIO ? APROBADO : REPROBADO;
    }

    /** La inasistencia no justificada pesa más que la nota: quien pierde por asistencia no aprueba. */
    public static String estado(Double parcial1, Double parcial2, Double examenFinal, boolean pierdePorAsistencia) {
        return pierdePorAsistencia ? PERDIDO_POR_FALTA_DE_ASISTENCIA
                : estado(parcial1, parcial2, examenFinal);
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
