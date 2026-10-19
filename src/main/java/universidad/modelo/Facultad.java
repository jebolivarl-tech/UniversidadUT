package universidad.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Facultad {

    private int idFacultad;
    private String nombre;
    private String decano;

    public Facultad() {
    }

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
