package cl.duoc.gymflow.catalog.error;

/**
 * La operación choca con el estado actual (sin cupos, cupo menor a lo ya ocupado, sala en uso...).
 * Se responde 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
