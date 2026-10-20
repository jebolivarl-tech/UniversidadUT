package universidad.servicio;

/**
 * Incumplimiento de una regla académica. Trae el código con el que debe responder la API:
 * 409 cuando el dato es válido pero la regla lo rechaza, 400 cuando falta una referencia.
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
