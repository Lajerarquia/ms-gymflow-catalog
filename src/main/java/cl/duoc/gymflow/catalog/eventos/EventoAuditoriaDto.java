package cl.duoc.gymflow.catalog.eventos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Evento de auditoría ("quién hizo qué, cuándo y desde dónde"), publicado en {@code audit.timeline}
 * (clave = {@code eventId}). Lo producen reservations y catalog; lo consume ms-gymflow-audit (grupo {@code gymflow-audit}).
 * <p>
 * La clave es el {@code eventId} porque el tópico es {@code compact,delete}: la compactación solo elimina copias
 * repetidas del MISMO evento (reintentos, replays) y nunca borra historia distinta. Cada servicio tiene su propia
 * copia de este record (no hay librería compartida): el JSON viaja sin cabeceras de tipo.
 *
 * @param eventId       UUID único del evento
 * @param occurredAt    cuándo ocurrió, en UTC
 * @param traceId       traza de la petición HTTP que lo originó
 * @param actorId       quién ({@code oid} de Azure AD)
 * @param actorName     nombre de quien lo hizo
 * @param action        qué hizo
 * @param entityType    sobre qué tipo de entidad
 * @param entityId      id de la entidad afectada
 * @param detail        descripción legible, en español
 * @param sourceService servicio que publicó el evento
 * @param sourceIp      IP del usuario final (cabecera X-Client-Ip del BFF); null si no se conoce
 */
public record EventoAuditoriaDto(
        @NotBlank String eventId,
        @NotNull Instant occurredAt,
        @NotBlank String traceId,
        @NotBlank String actorId,
        @NotBlank String actorName,
        @NotNull Accion action,
        @NotNull TipoEntidad entityType,
        @NotBlank String entityId,
        @NotBlank String detail,
        @NotBlank String sourceService,
        String sourceIp) {

    public enum Accion { CREAR, CAMBIAR_ESTADO, EDITAR, ELIMINAR }

    public enum TipoEntidad { RESERVA, CLASE, SALA }
}
