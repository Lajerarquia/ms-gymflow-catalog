package cl.duoc.gymflow.catalog.error;

/** La clase o sala pedida no existe. Se responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
