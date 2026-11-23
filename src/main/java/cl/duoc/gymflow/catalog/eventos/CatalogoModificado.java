package cl.duoc.gymflow.catalog.eventos;

import cl.duoc.gymflow.catalog.config.FiltroContexto;
import cl.duoc.gymflow.catalog.eventos.EventoAuditoriaDto.Accion;
import cl.duoc.gymflow.catalog.eventos.EventoAuditoriaDto.TipoEntidad;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Evento <b>interno</b> de la aplicación (no viaja por Kafka): "un usuario creó, editó o eliminó una clase o sala".
 * ClaseService y SalaService lo publican DENTRO de su transacción; {@link PublicadorAuditoria} lo recibe con
 * {@code @TransactionalEventListener(AFTER_COMMIT)}: si hay rollback (por ejemplo un 409), no se audita nada.
 * <p>
 * Toma del MDC (lo llena {@link FiltroContexto}) quién lo hizo, la traza y la IP, porque la publicación a Kafka
 * ocurre en otro hilo.
 */
public record CatalogoModificado(Accion accion, TipoEntidad tipoEntidad, Long entidadId, String detalle,
                                 String actorId, String actorNombre, Instant ocurrio, String traceId, String ipCliente) {

    /** Si no llegó la identidad (no pasa a través del BFF), el evento igual se registra, marcado como desconocido. */
    static final String DESCONOCIDO_ID = "desconocido";
    static final String DESCONOCIDO_NOMBRE = "Usuario desconocido";

    public static CatalogoModificado de(Accion accion, TipoEntidad tipoEntidad, Long entidadId, String detalle) {
        String actorId = MDC.get(FiltroContexto.MDC_USUARIO_ID);
        String actorNombre = MDC.get(FiltroContexto.MDC_USUARIO_NOMBRE);
        String traceId = MDC.get(FiltroContexto.MDC_TRAZA);
        return new CatalogoModificado(accion, tipoEntidad, entidadId, detalle,
                actorId != null ? actorId : DESCONOCIDO_ID,
                actorNombre != null ? actorNombre : (actorId != null ? actorId : DESCONOCIDO_NOMBRE),
                Instant.now(),
                traceId != null ? traceId : UUID.randomUUID().toString(),
                MDC.get(FiltroContexto.MDC_IP));
    }
}
