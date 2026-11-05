package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una facultad: su nombre y el decano que la representa (campo de relleno, puede quedar vacío).
 * En MySQL el nombre no se repite, así que no se pueden crear dos facultades iguales.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Facultad {

    private int idFacultad;
    private String nombre;
    private String decano;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
    public Facultad() {
    }

    /** Alta: el id lo pone la base de datos. */
    public Facultad(String nombre, String decano) {
        this.nombre = nombre;
        this.decano = decano;
    }

    public int getIdFacultad() { return idFacultad; }
    public void setIdFacultad(int idFacultad) { this.idFacultad = idFacultad; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDecano() { return decano; }
    public void setDecano(String decano) { this.decano = decano; }
}
