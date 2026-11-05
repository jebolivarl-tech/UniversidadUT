package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.sql.Date;

/**
 * Un periodo lectivo, por ejemplo "2025-2", con sus fechas de inicio y fin.
 * El estado manda en las reglas: solo con ACTIVO se admiten matrículas y cambios de sección, y quien
 * intente operar sobre un periodo cerrado recibe un conflicto en la API en vez de un guardado a medias.
 * MySQL no deja que la fecha de fin caiga antes de la de inicio, y un nombre de periodo no se repite.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodoAcademico {

    // Los tres valores que admite la columna estado. Solo ACTIVO deja operar: INACTIVO y CERRADO frenan
    // las inscripciones nuevas y los cambios de sección.
    public static final String ACTIVO = "ACTIVO";
    public static final String INACTIVO = "INACTIVO";
    public static final String CERRADO = "CERRADO";

    private int idPeriodo;
    private String nombre;
    // java.sql.Date: día calendario, sin hora, como lo guarda MySQL.
    private Date fechaInicio;
    private Date fechaFin;
    private String estado;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public PeriodoAcademico() {
    }

    /** Alta: el id lo pone la base de datos. */
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
