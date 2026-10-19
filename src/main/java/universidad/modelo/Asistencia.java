package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.sql.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Asistencia {

    public static final String ASISTIO = "ASISTIO";
    public static final String FALTO = "FALTO";
    public static final String TARDANZA = "TARDANZA";
    public static final String JUSTIFICADO = "JUSTIFICADO";

    private int idAsistencia;
    private int idEstudiante;
    private int idSeccion;
    private Date fecha;
    private String estado;

    public Asistencia() {
    }

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
