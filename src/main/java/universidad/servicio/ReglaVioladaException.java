package universidad.servicio;

/**
 * Se lanza cuando una regla académica no deja seguir: la sección está llena, el periodo no está
 * abierto, el estudiante ya cursa esa materia. Lleva dentro el código con el que la API REST
 * (los enlaces web a los que la página manda sus peticiones) debe responder: 409 si el dato es
 * válido pero la regla lo rechaza, 400 si el formulario apunta a un registro que no existe.
 */
public class ReglaVioladaException extends RuntimeException {

    private final int httpStatus;

    private ReglaVioladaException(int httpStatus, String mensaje) {
        super(mensaje);
        this.httpStatus = httpStatus;
    }

    /** La regla no deja operar: el periodo está cerrado, la sección está llena, el cupo no alcanza. */
    public static ReglaVioladaException deConflicto(String mensaje) {
        return new ReglaVioladaException(409, mensaje);
    }

    /** El formulario apunta a un estudiante, sección, curso o periodo que no existe. */
    public static ReglaVioladaException deReferencia(String mensaje) {
        return new ReglaVioladaException(400, mensaje);
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
