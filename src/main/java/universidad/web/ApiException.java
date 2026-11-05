package universidad.web;

/**
 * Error que el servidor lanza a propósito cuando una petición sale mal, frente al fallo inesperado.
 * La API REST son las rutas HTTP que el navegador llama para traer datos en vez de pedir una página.
 * Cuando algo de esas rutas lanza este error, la respuesta es un JSON (el texto con llaves que
 * intercambian navegador y servidor) de la forma {"error": "..."} y con el código HTTP guardado aquí.
 * Los que se usan: 400 dato mal formado, 401 sin sesión, 403 sin permiso, 404 no existe, 409 choca
 * con un dato ya registrado, 415 cuerpo con formato equivocado, 429 demasiados intentos, 500 problema
 * del servidor.
 * Basta con lanzarlo desde cualquier ruta: un solo manejador en {@link universidad.ControladorWeb}
 * arma la respuesta, así que ningún controlador repite el mismo bloque de captura.
 */
public class ApiException extends RuntimeException {

    /** Código HTTP con el que responde la API cuando este error sube hasta el manejador. */
    private final int status;

    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
