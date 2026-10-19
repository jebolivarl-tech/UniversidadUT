package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.sql.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodoAcademico {

    public static final String ACTIVO = "ACTIVO";
    public static final String INACTIVO = "INACTIVO";
    public static final String CERRADO = "CERRADO";

    private int idPeriodo;
    private String nombre;
    private Date fechaInicio;
    private Date fechaFin;
    private String estado;

    public PeriodoAcademico() {
    }

    public PeriodoAcademico(String nombre, Date fechaInicio, Date fechaFin, String estado) {
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public int getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(int idPeriodo) { this.idPeriodo = idPeriodo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public Date getFechaFin() { return fechaFin; }
    public void setFechaFin(Date fechaFin) { this.fechaFin = fechaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
