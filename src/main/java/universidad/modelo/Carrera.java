package universidad.modelo;

public class Carrera {
    private int idCarrera;
    private String nombre;
    private int idFacultad;

    // Constructor vacío
    public Carrera() {}

    // Constructor completo
    public Carrera(int idCarrera, String nombre, int idFacultad) {
        this.idCarrera = idCarrera;
        this.nombre = nombre;
        this.idFacultad = idFacultad;
    }

    // Constructor sin ID (Para registrar nuevas carreras)
    public Carrera(String nombre, int idFacultad) {
        this.nombre = nombre;
        this.idFacultad = idFacultad;
    }

    // Getters y Setters
    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getIdFacultad() { return idFacultad; }
    public void setIdFacultad(int idFacultad) { this.idFacultad = idFacultad; }

    // Necesario para que los combos muestren el nombre de la carrera
    @Override
    public String toString() {
        return nombre;
    }
}