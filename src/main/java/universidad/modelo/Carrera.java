package universidad.modelo;

/**
 * Una carrera: su nombre y la facultad a la que pertenece.
 * No guarda el nombre de la facultad sino su id, que es la llave de su fila en MySQL. La pantalla
 * muestra el nombre leyéndolo de la lista de facultades; por aquí solo viaja el número.
 */
public class Carrera {
    private int idCarrera;
    private String nombre;
    private int idFacultad;

    // Vacío a propósito: así la arma Jackson al leer el JSON y el DAO al leer la fila.
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

    /** En las listas desplegables de la web se muestra este texto; si no, saldría el nombre de la clase. */
    @Override
    public String toString() {
        return nombre;
    }
}