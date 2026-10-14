package universidad.web;

/** Error controlado de la API: se traduce a una respuesta JSON {"error": "..."} con el código indicado. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
