package cl.duoc.gymflow.catalog.eventos;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica en {@code audit.timeline} un {@link EventoAuditoriaDto} por cada clase o sala creada, editada o eliminada,
 * <b>después del commit</b>: nunca se audita un cambio que no quedó guardado. Clave = {@code eventId}.
 * No bloquea la respuesta ni la hace fallar si Kafka está caído (ver {@link EnviadorKafka}).
 */
@Component
public class PublicadorAuditoria {

    public static final String SERVICIO = "ms-gymflow-catalog";

    private final EnviadorKafka enviador;

    public PublicadorAuditoria(EnviadorKafka enviador) {
        this.enviador = enviador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alModificarCatalogo(CatalogoModificado cambio) {
        EventoAuditoriaDto evento = aEvento(cambio);
        enviador.enviar(KafkaConfig.TOPICO_AUDITORIA, evento.eventId(), evento, evento.traceId(),
                "AUDITORIA_" + evento.action().name());
    }

    static EventoAuditoriaDto aEvento(CatalogoModificado cambio) {
        return new EventoAuditoriaDto(
                UUID.randomUUID().toString(),
                cambio.ocurrio(),
                cambio.traceId(),
                cambio.actorId(),
                cambio.actorNombre(),
                cambio.accion(),
                cambio.tipoEntidad(),
                String.valueOf(cambio.entidadId()),
                cambio.detalle(),
                SERVICIO,
                cambio.ipCliente());
    }
}
