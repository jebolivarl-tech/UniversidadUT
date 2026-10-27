package universidad.modelo;

public class Carrera {
    private int idCarrera;
    private String nombre;
    private int idFacultad;

    public Carrera() {}

    public Carrera(int idCarrera, String nombre, int idFacultad) {
        this.idCarrera = idCarrera;
        this.nombre = nombre;
        this.idFacultad = idFacultad;
    }

    /** Alta: el id lo pone la base de datos. */
    public Carrera(String nombre, int idFacultad) {
        this.nombre = nombre;
        this.idFacultad = idFacultad;
    }

    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getIdFacultad() { return idFacultad; }
    public void setIdFacultad(int idFacultad) { this.idFacultad = idFacultad; }

    /** Los combos de Swing y de la web muestran la entidad con toString(); sin esto saldría la clase. */
    @Override
    public String toString() {
        return nombre;
    }
}