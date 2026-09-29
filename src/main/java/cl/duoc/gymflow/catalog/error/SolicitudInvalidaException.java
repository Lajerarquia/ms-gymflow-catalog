package cl.duoc.gymflow.catalog.error;

/** Los datos enviados no tienen sentido (por ejemplo, una sala que no existe). Se responde 400. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
