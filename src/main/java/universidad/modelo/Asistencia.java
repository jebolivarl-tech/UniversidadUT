package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.sql.Date;

/**
 * Un control de asistencia: un estudiante, una sección, un día y qué pasó ese día.
 * De aquí sale la cuenta que decide si alguien pierde el curso por inasistencia, así que esta clase
 * no calcula nada: solo guarda el dato tal como está en su fila de MySQL.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Asistencia {

    // Los cuatro valores que acepta la columna estado. Solo FALTO descuenta; la tardanza y la falta
    // justificada se registran pero no pesan en el cálculo.
    public static final String ASISTIO = "ASISTIO";
    public static final String FALTO = "FALTO";
    public static final String TARDANZA = "TARDANZA";
    public static final String JUSTIFICADO = "JUSTIFICADO";

    private int idAsistencia;
    private int idEstudiante;
    private int idSeccion;
    // Fecha sin hora: es java.sql.Date, el tipo de fecha que devuelve JDBC al leer la fila de MySQL.
    private Date fecha;
    private String estado;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Asistencia() {
    }

    /** Alta: todavía no lleva id, el que la identifica lo pone la base de datos al insertar. */
    public Asistencia(int idEstudiante, int idSeccion, Date fecha, String estado) {
        this.idEstudiante = idEstudiante;
        this.idSeccion = idSeccion;
        this.fecha = fecha;
        this.estado = estado;
    }

    public int getIdAsistencia() { return idAsistencia; }
    public void setIdAsistencia(int idAsistencia) { this.idAsistencia = idAsistencia; }

    public int getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public int getIdSeccion() { return idSeccion; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
