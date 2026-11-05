package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * La inscripción de un estudiante en una sección de un periodo: es lo que lo matricula en un curso.
 * La fila guarda los ids de la sección y del curso y el nombre del periodo, pero nadie los escribe a
 * mano: el servicio de dominio los copia de la sección elegida, para que ninguna matrícula contradiga
 * al grupo en el que está. La nota definitiva tampoco la decide quien llena el formulario: llega de la
 * calificación y se sincroniza desde el servicio, para que no exista una segunda versión de la misma
 * nota.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Matricula {

    // Los cuatro estados que admite la columna en MySQL; arranca en EN_CURSO.
    public static final String EN_CURSO = "EN_CURSO";
    public static final String APROBADO = "APROBADO";
    public static final String REPROBADO = "REPROBADO";
    /** Se queda sin evaluación definitiva por inasistencias no justificadas, aunque le alcanzara la nota. */
    public static final String PERDIDO_POR_FALTA_DE_ASISTENCIA = "PERDIDO_POR_FALTA_DE_ASISTENCIA";

    private int idMatricula;
    private int idEstudiante;
    private int idSeccion;
    private int idCurso;
    private String periodo;
    // Mientras cursa no hay nota, y ese vacío no es un cero: 0 sería una nota presentada y suspensa.
    private Double nota;
    private String estado = EN_CURSO;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
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
